package com.codereview.agent.agent;

import com.codereview.agent.github.GitHubService;
import com.codereview.agent.kafka.event.ReviewEvent;
import com.codereview.agent.persistence.entity.ReviewFinding;
import com.codereview.agent.persistence.entity.ReviewJob;
import com.codereview.agent.persistence.repository.ReviewFindingRepository;
import com.codereview.agent.persistence.repository.ReviewJobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Orchestrates a single review:
 *   1. Mark job RUNNING
 *   2. Fetch the diff from GitHub
 *   3. Invoke the LangChain4j agent (which drives the Claude tool-use loop)
 *   4. Parse each JSON finding line as it streams back
 *   5. Persist findings, post inline GitHub comments, emit SSE events
 *   6. Mark job COMPLETED or FAILED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewOrchestrator {

    private final CodeReviewerAgent agent;
    private final GitHubService gitHubService;
    private final ReviewJobRepository jobRepo;
    private final ReviewFindingRepository findingRepo;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper;
    private final MeterRegistry meterRegistry;

    @Value("${kafka-topics.review-events}")
    private String reviewEventsTopic;

    public void runReview(UUID jobId, String githubFullName, int prNumber, String headSha) {
        Timer.Sample sample = Timer.start(meterRegistry);
        ReviewJob job = jobRepo.findById(jobId).orElseThrow();
        job.setStatus(ReviewJob.Status.RUNNING);
        job.setStartedAt(OffsetDateTime.now());
        jobRepo.save(job);
        emit(jobId, ReviewEvent.Type.JOB_STARTED, "");

        try {
            String diff = gitHubService.fetchPullRequestDiff(githubFullName, prNumber);

            // Single blocking call — LangChain4j drives the tool-use loop internally.
            // For true token-by-token streaming, swap CodeReviewerAgent to return TokenStream
            // and emit REASONING_CHUNK events from the callback.
            String result = agent.reviewPullRequest(githubFullName, String.valueOf(prNumber), headSha, diff);

            // Parse JSON lines; ignore anything that isn't a valid finding object.
            for (String line : result.split("\\r?\\n")) {
                line = line.trim();
                if (line.isEmpty() || line.equals("<REVIEW_COMPLETE>")) continue;
                if (!line.startsWith("{")) continue;
                try {
                    JsonNode node = mapper.readTree(line);
                    persistAndPostFinding(job, node);
                } catch (Exception e) {
                    log.warn("Skipping malformed finding line: {}", line, e);
                }
            }

            job.setStatus(ReviewJob.Status.COMPLETED);
            job.setCompletedAt(OffsetDateTime.now());
            jobRepo.save(job);
            emit(jobId, ReviewEvent.Type.JOB_COMPLETED, "");
        } catch (Exception e) {
            log.error("Review job {} failed", jobId, e);
            job.setStatus(ReviewJob.Status.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(OffsetDateTime.now());
            jobRepo.save(job);
            emit(jobId, ReviewEvent.Type.JOB_FAILED, e.getMessage());
        } finally {
            sample.stop(meterRegistry.timer("review.duration",
                    "status", job.getStatus().name()));
        }
    }

    private void persistAndPostFinding(ReviewJob job, JsonNode node) throws Exception {
        ReviewFinding finding = ReviewFinding.builder()
                .jobId(job.getId())
                .filePath(node.path("file").asText())
                .lineNumber(node.path("line").isNull() ? null : node.path("line").asInt())
                .severity(ReviewFinding.Severity.valueOf(node.path("severity").asText("INFO")))
                .category(ReviewFinding.Category.valueOf(node.path("category").asText("MAINTAINABILITY")))
                .message(node.path("message").asText())
                .suggestedFix(node.path("suggested_fix").asText(null))
                .postedToGithub(false)
                .build();
        finding = findingRepo.save(finding);

        // Post inline comment on GitHub. Don't fail the whole review if this fails.
        try {
            if (finding.getLineNumber() != null) {
                gitHubService.postInlineComment(
                        /* fullName */ "", // TODO: resolve from job.repositoryId
                        job.getPrNumber(),
                        job.getHeadSha(),
                        finding.getFilePath(),
                        finding.getLineNumber(),
                        formatComment(finding));
                finding.setPostedToGithub(true);
                findingRepo.save(finding);
            }
        } catch (Exception e) {
            log.warn("Failed to post GitHub comment for finding {}", finding.getId(), e);
        }

        emit(job.getId(), ReviewEvent.Type.FINDING_EMITTED, mapper.writeValueAsString(finding));
    }

    private String formatComment(ReviewFinding f) {
        StringBuilder sb = new StringBuilder();
        sb.append("**[").append(f.getSeverity()).append(" · ").append(f.getCategory()).append("]** ");
        sb.append(f.getMessage());
        if (f.getSuggestedFix() != null && !f.getSuggestedFix().isEmpty()) {
            sb.append("\n\n```suggestion\n").append(f.getSuggestedFix()).append("\n```");
        }
        sb.append("\n\n<sub>🤖 Posted by Code Review Agent</sub>");
        return sb.toString();
    }

    private void emit(UUID jobId, ReviewEvent.Type type, String payload) {
        kafka.send(reviewEventsTopic, jobId.toString(), new ReviewEvent(jobId, type, payload));
    }
}

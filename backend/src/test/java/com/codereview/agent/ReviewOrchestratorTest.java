package com.codereview.agent;

import com.codereview.agent.agent.CodeReviewerAgent;
import com.codereview.agent.agent.ReviewOrchestrator;
import com.codereview.agent.github.GitHubService;
import com.codereview.agent.persistence.entity.ReviewFinding;
import com.codereview.agent.persistence.entity.ReviewJob;
import com.codereview.agent.persistence.repository.ReviewFindingRepository;
import com.codereview.agent.persistence.repository.ReviewJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewOrchestratorTest {

    @Mock CodeReviewerAgent agent;
    @Mock GitHubService gitHubService;
    @Mock ReviewJobRepository jobRepo;
    @Mock ReviewFindingRepository findingRepo;
    @Mock KafkaTemplate<String, Object> kafka;

    ReviewOrchestrator orchestrator;
    UUID jobId;
    ReviewJob job;

    @BeforeEach
    void setUp() {
        orchestrator = new ReviewOrchestrator(
                agent, gitHubService, jobRepo, findingRepo, kafka,
                new ObjectMapper(), new SimpleMeterRegistry());
        jobId = UUID.randomUUID();
        job = ReviewJob.builder()
                .id(jobId)
                .repositoryId(UUID.randomUUID())
                .prNumber(42)
                .headSha("abc123")
                .status(ReviewJob.Status.QUEUED)
                .build();
    }

    @Test
    void parsesFindingsFromAgentOutput() throws Exception {
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(ReviewJob.class))).thenAnswer(i -> i.getArgument(0));
        when(findingRepo.save(any(ReviewFinding.class))).thenAnswer(i -> {
            ReviewFinding f = i.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });
        when(gitHubService.fetchPullRequestDiff(anyString(), anyInt())).thenReturn("fake diff");

        String agentOutput = """
                {"file":"src/Foo.java","line":10,"severity":"HIGH","category":"SECURITY","message":"SQL injection","suggested_fix":"use parameters"}
                some reasoning text here, not a finding
                {"file":"src/Bar.java","line":25,"severity":"LOW","category":"STYLE","message":"naming"}
                <REVIEW_COMPLETE>
                """;
        when(agent.reviewPullRequest(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(agentOutput);

        orchestrator.runReview(jobId, "octocat/hello", 42, "abc123");

        ArgumentCaptor<ReviewFinding> captor = ArgumentCaptor.forClass(ReviewFinding.class);
        // Each finding is saved twice: once to assign an id, then again after
        // the GitHub inline comment is posted (to flip postedToGithub=true).
        verify(findingRepo, times(4)).save(captor.capture());
        List<ReviewFinding> distinctFindings = captor.getAllValues().stream()
                .distinct()
                .toList();
        assertThat(distinctFindings).hasSize(2);
        assertThat(distinctFindings).extracting(ReviewFinding::getSeverity)
                .containsExactly(ReviewFinding.Severity.HIGH, ReviewFinding.Severity.LOW);
        assertThat(distinctFindings.get(0).getMessage()).isEqualTo("SQL injection");
    }

    @Test
    void marksJobFailedWhenAgentThrows() throws Exception {
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(ReviewJob.class))).thenAnswer(i -> i.getArgument(0));
        when(gitHubService.fetchPullRequestDiff(anyString(), anyInt()))
                .thenThrow(new RuntimeException("GitHub API down"));

        orchestrator.runReview(jobId, "octocat/hello", 42, "abc123");

        ArgumentCaptor<ReviewJob> captor = ArgumentCaptor.forClass(ReviewJob.class);
        verify(jobRepo, atLeast(2)).save(captor.capture());
        ReviewJob finalState = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(finalState.getStatus()).isEqualTo(ReviewJob.Status.FAILED);
        assertThat(finalState.getErrorMessage()).contains("GitHub API down");
    }
    @Test
    void parsesZeroFindingsWhenAgentReturnsOnlyCompleteMarker() throws Exception {
        // Arrange — set up the job to be loaded from the database
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(ReviewJob.class))).thenAnswer(i -> i.getArgument(0));
        when(gitHubService.fetchPullRequestDiff(anyString(), anyInt())).thenReturn("fake diff");

        // Agent returns nothing but the completion marker — a clean review with no issues
        String agentOutput = "<REVIEW_COMPLETE>\n";
        when(agent.reviewPullRequest(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(agentOutput);

        // Act — run the review
        orchestrator.runReview(jobId, "octocat/hello", 42, "abc123");

        // Assert — no findings were saved to the database
        verify(findingRepo, never()).save(any(ReviewFinding.class));
    }
}

package com.codereview.agent.github;

import com.codereview.agent.kafka.event.ReviewJobRequested;
import com.codereview.agent.persistence.entity.ReviewJob;
import com.codereview.agent.persistence.repository.ReviewJobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Receives webhooks from GitHub. Responds fast (<10s) by enqueueing work to Kafka.
 *
 * GitHub event types handled:
 *   - pull_request (action: opened, synchronize, reopened)
 */
@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Slf4j
public class GitHubWebhookController {

    private final GitHubService gitHubService;
    private final ReviewJobRepository jobRepo;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper;

    @Value("${kafka-topics.review-jobs}")
    private String reviewJobsTopic;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload) throws Exception {

        // To verify the request that came from the signature that we created that is HMAC-SHA256
        if (!gitHubService.verifySignature(payload, signature)) {
            log.warn("Rejected webhook with invalid signature");
            return ResponseEntity.status(401).body("invalid signature");
        }

        // Our job is only related to the pull requests
        if (!"pull_request".equals(event)) {
            return ResponseEntity.ok("ignored: " + event);
        }

        JsonNode root = mapper.readTree(payload);
        String action = root.path("action").asText();
        // What actually matters is that the code is actually changed else ignore
        if (!("opened".equals(action) || "synchronize".equals(action) || "reopened".equals(action))) {
            return ResponseEntity.ok("ignored action: " + action);
        }

        // get the PR details from JSON payload
        String fullName = root.path("repository").path("full_name").asText();
        int prNumber = root.path("pull_request").path("number").asInt();
        String headSha = root.path("pull_request").path("head").path("sha").asText();
        long installationId = root.path("installation").path("id").asLong();

        // TODO: look up repository UUID by full_name; upsert if missing
        UUID repoId = UUID.nameUUIDFromBytes(fullName.getBytes());

        // Save the new job to the database with status QUEUED
        ReviewJob job = jobRepo.save(ReviewJob.builder()
                .repositoryId(repoId)
                .prNumber(prNumber)
                .headSha(headSha)
                .status(ReviewJob.Status.QUEUED)
                .build());

        // Push the job to Kafka so a worker can pick it up and run the review
        kafka.send(reviewJobsTopic, job.getId().toString(),
                new ReviewJobRequested(job.getId(), repoId, fullName, prNumber, headSha, installationId));

        log.info("Enqueued review job {} for {} #{}", job.getId(), fullName, prNumber);
        // 202 Accepted — we've queued the work, the actual review runs asynchronously
        return ResponseEntity.accepted().body(job.getId().toString());
    }
}

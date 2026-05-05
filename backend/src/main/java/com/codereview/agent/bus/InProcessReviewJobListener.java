package com.codereview.agent.bus;

import com.codereview.agent.agent.ReviewOrchestrator;
import com.codereview.agent.kafka.event.ReviewJobRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * In-process counterpart to {@link com.codereview.agent.kafka.ReviewJobConsumer}.
 *
 * <p>Listens for {@link ReviewJobRequested} events on Spring's in-process
 * event bus and runs the review on the {@code reviewExecutor} thread pool —
 * keeping the webhook controller's HTTP thread free to return 202 Accepted
 * within GitHub's 10-second deadline.
 */
@Component
@ConditionalOnProperty(name = "review-bus.type", havingValue = "in-process")
@RequiredArgsConstructor
@Slf4j
public class InProcessReviewJobListener {

    private final ReviewOrchestrator orchestrator;

    @Async("reviewExecutor")
    @EventListener
    public void onJob(ReviewJobRequested job) {
        log.info("In-process: handling job {} for {} #{}",
                job.jobId(), job.githubFullName(), job.prNumber());
        try {
            orchestrator.runReview(job.jobId(), job.githubFullName(), job.prNumber(), job.headSha());
        } catch (Exception e) {
            // Orchestrator handles its own failure persistence; log and continue.
            log.error("Unhandled exception running job {}", job.jobId(), e);
        }
    }
}

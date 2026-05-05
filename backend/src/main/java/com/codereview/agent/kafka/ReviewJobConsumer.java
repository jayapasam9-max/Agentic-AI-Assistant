package com.codereview.agent.kafka;

import com.codereview.agent.agent.ReviewOrchestrator;
import com.codereview.agent.kafka.event.ReviewJobRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that pulls review jobs off the {@code review-jobs} topic
 * and hands them to the orchestrator.
 *
 * <p>Disabled when {@code review-bus.type=in-process} (cloud-free profile);
 * see {@link com.codereview.agent.bus.InProcessReviewJobListener} for the
 * in-process counterpart.
 */
@Component
@ConditionalOnProperty(name = "review-bus.type", havingValue = "kafka", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ReviewJobConsumer {

    private final ReviewOrchestrator orchestrator;

    @KafkaListener(
            topics = "${kafka-topics.review-jobs}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ReviewJobRequested event) {
        log.info("Picked up review job {} for {} #{}",
                event.jobId(), event.githubFullName(), event.prNumber());
        try {
            orchestrator.runReview(event.jobId(), event.githubFullName(), event.prNumber(), event.headSha());
        } catch (Exception e) {
            // Orchestrator handles its own failure persistence; log and let Kafka ack.
            log.error("Unhandled exception running job {}", event.jobId(), e);
        }
    }
}

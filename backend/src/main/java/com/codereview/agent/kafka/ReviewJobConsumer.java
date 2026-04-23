package com.codereview.agent.kafka;

import com.codereview.agent.agent.ReviewOrchestrator;
import com.codereview.agent.kafka.event.ReviewJobRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
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

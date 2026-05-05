package com.codereview.agent.bus;

import com.codereview.agent.kafka.event.ReviewEvent;
import com.codereview.agent.kafka.event.ReviewJobRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed implementation of {@link ReviewBus}. Owns the Kafka topic
 * names so callers (the webhook controller, the orchestrator) don't need to
 * know about Kafka at all.
 *
 * <p>The job's id is used as the partition key so all events for a given PR
 * land on the same partition and stay ordered.
 *
 * <p>Activated by default. Set {@code review-bus.type=in-process} to swap in
 * {@link InProcessReviewBus} (used by the {@code cloud-free} profile).
 */
@Component
@ConditionalOnProperty(name = "review-bus.type", havingValue = "kafka", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaReviewBus implements ReviewBus {

    private final KafkaTemplate<String, Object> kafka;

    @Value("${kafka-topics.review-jobs}")
    private String reviewJobsTopic;

    @Value("${kafka-topics.review-events}")
    private String reviewEventsTopic;

    @Override
    public void publishJob(ReviewJobRequested job) {
        kafka.send(reviewJobsTopic, job.jobId().toString(), job);
    }

    @Override
    public void publishEvent(ReviewEvent event) {
        kafka.send(reviewEventsTopic, event.jobId().toString(), event);
    }
}

package com.codereview.agent.bus;

import com.codereview.agent.kafka.event.ReviewEvent;
import com.codereview.agent.kafka.event.ReviewJobRequested;

/**
 * Abstraction over the message bus that carries review work and review progress.
 *
 * <p>Two implementations are wired in by Spring depending on the active profile:
 * <ul>
 *   <li>{@link KafkaReviewBus} — the default. Publishes to the Kafka topics
 *       configured under {@code kafka-topics.*}.</li>
 *   <li>An in-process implementation backed by {@code ApplicationEventPublisher}
 *       — added in a later phase to support free-tier deployments without Kafka.</li>
 * </ul>
 *
 * <p>Callers depend on this interface (not on {@code KafkaTemplate} directly)
 * so the orchestrator and webhook controller don't need to know which transport
 * is in use.
 */
public interface ReviewBus {

    /**
     * Publish a job request so a worker (Kafka consumer or in-process listener)
     * can pick it up and run the review asynchronously.
     */
    void publishJob(ReviewJobRequested job);

    /**
     * Publish a progress event for the dashboard's SSE stream.
     */
    void publishEvent(ReviewEvent event);
}

package com.codereview.agent.bus;

import com.codereview.agent.kafka.event.ReviewEvent;
import com.codereview.agent.kafka.event.ReviewJobRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * In-process implementation of {@link ReviewBus}. Forwards events through
 * Spring's own {@link ApplicationEventPublisher} so beans annotated with
 * {@code @EventListener} (e.g., {@link InProcessReviewJobListener}) can pick
 * them up without any external broker.
 *
 * <p>Activated when {@code review-bus.type=in-process}, used by the
 * {@code cloud-free} Spring profile to support free-tier deployments where
 * running Kafka would cost money.
 */
@Component
@ConditionalOnProperty(name = "review-bus.type", havingValue = "in-process")
@RequiredArgsConstructor
public class InProcessReviewBus implements ReviewBus {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publishJob(ReviewJobRequested job) {
        publisher.publishEvent(job);
    }

    @Override
    public void publishEvent(ReviewEvent event) {
        publisher.publishEvent(event);
    }
}

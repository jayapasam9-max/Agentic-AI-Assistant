package com.codereview.agent.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables {@code @Async} processing for the in-process review path.
 *
 * <p>The {@code reviewExecutor} bean is intentionally small — Render's free
 * tier gives us ~0.1 vCPU and 512 MB RAM, so running multiple reviews
 * concurrently would just thrash. We serialize jobs and let the queue
 * absorb bursts.
 *
 * <p>Only active when {@code review-bus.type=in-process} (cloud-free profile).
 * In Kafka mode, concurrency is handled by the Kafka listener container
 * factory in {@link KafkaConfig}.
 */
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "review-bus.type", havingValue = "in-process")
public class AsyncConfig {

    @Bean(name = "reviewExecutor")
    public Executor reviewExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("review-");
        executor.initialize();
        return executor;
    }
}

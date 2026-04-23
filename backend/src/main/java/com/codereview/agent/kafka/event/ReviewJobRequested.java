package com.codereview.agent.kafka.event;

import java.util.UUID;

/**
 * Published by the webhook controller when a PR event warrants review.
 * Consumed by ReviewJobConsumer, which runs the agent.
 */
public record ReviewJobRequested(
        UUID jobId,
        UUID repositoryId,
        String githubFullName,
        int prNumber,
        String headSha,
        long installationId
) {}

package com.codereview.agent.api.dto;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public JSON shape for a single row of the dashboard reviews table.
 *
 * <p>Adds two derived fields on top of {@link ReviewSummaryRow}:
 * <ul>
 *   <li>{@code durationMs} — completed_at − started_at, or null if still running</li>
 *   <li>{@code tokensTotal} — tokens_input + tokens_output</li>
 * </ul>
 */
public record ReviewSummaryDto(
        UUID id,
        String repoFullName,
        Integer prNumber,
        String headSha,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Long durationMs,
        Integer tokensInput,
        Integer tokensOutput,
        Integer tokensTotal) {

    public static ReviewSummaryDto from(ReviewSummaryRow row) {
        Long duration = null;
        if (row.getStartedAt() != null && row.getCompletedAt() != null) {
            duration = Duration.between(row.getStartedAt(), row.getCompletedAt()).toMillis();
        }
        int tIn = row.getTokensInput() == null ? 0 : row.getTokensInput();
        int tOut = row.getTokensOutput() == null ? 0 : row.getTokensOutput();
        return new ReviewSummaryDto(
                row.getId(),
                row.getRepoFullName(),
                row.getPrNumber(),
                row.getHeadSha(),
                row.getStatus(),
                row.getCreatedAt(),
                row.getStartedAt(),
                row.getCompletedAt(),
                duration,
                tIn,
                tOut,
                tIn + tOut);
    }
}

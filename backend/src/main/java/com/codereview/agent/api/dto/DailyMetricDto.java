package com.codereview.agent.api.dto;

import java.time.LocalDate;

/**
 * Public JSON shape for one day on the metrics dashboard.
 *
 * <p>Adds a derived {@code costUsd} field on top of {@link DailyMetricRow},
 * computed from the per-million-token rates below. These are deliberately
 * conservative defaults for an Opus-class model — adjust if you swap the
 * configured {@code anthropic.model}.
 */
public record DailyMetricDto(
        LocalDate day,
        Long reviews,
        Long completed,
        Long failed,
        Long tokensInput,
        Long tokensOutput,
        Long tokensTotal,
        Double costUsd) {

    /** USD per million input tokens. Approximate Opus-class pricing. */
    private static final double INPUT_USD_PER_MTOK = 15.0;
    /** USD per million output tokens. Approximate Opus-class pricing. */
    private static final double OUTPUT_USD_PER_MTOK = 75.0;

    public static DailyMetricDto from(DailyMetricRow row) {
        long tIn = row.getTokensInput() == null ? 0 : row.getTokensInput();
        long tOut = row.getTokensOutput() == null ? 0 : row.getTokensOutput();
        double cost =
                (tIn / 1_000_000.0) * INPUT_USD_PER_MTOK
                        + (tOut / 1_000_000.0) * OUTPUT_USD_PER_MTOK;
        return new DailyMetricDto(
                row.getDay(),
                row.getReviews(),
                row.getCompleted(),
                row.getFailed(),
                tIn,
                tOut,
                tIn + tOut,
                // Round to 4 decimal places — keeps tiny costs visible
                // ($0.0021) but doesn't drown the JSON in noise.
                Math.round(cost * 10_000.0) / 10_000.0);
    }
}

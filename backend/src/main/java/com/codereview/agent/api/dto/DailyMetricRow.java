package com.codereview.agent.api.dto;

import java.time.LocalDate;

/**
 * Spring Data interface projection for one bucket of the daily metrics
 * aggregation, used by {@code GET /api/public/metrics/daily}.
 *
 * <p>The {@code day} bucket is computed in Postgres via
 * {@code date_trunc('day', created_at)::date} and surfaces as a {@link LocalDate}.
 */
public interface DailyMetricRow {
    LocalDate getDay();

    Long getReviews();

    Long getCompleted();

    Long getFailed();

    Long getTokensInput();

    Long getTokensOutput();
}

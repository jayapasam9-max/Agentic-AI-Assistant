package com.codereview.agent.api;

import com.codereview.agent.api.dto.DailyMetricDto;
import com.codereview.agent.persistence.repository.ReviewJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Aggregate read endpoints for the dashboard's metrics view.
 *
 * <p>Lives under {@code /api/public/metrics/*} alongside the existing
 * {@code /api/public/reviews/*} namespace — same CORS rule covers both.
 */
@RestController
@RequestMapping("/api/public/metrics")
@RequiredArgsConstructor
public class PublicMetricsController {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 90;
    private static final int DEFAULT_DAYS = 14;

    private final ReviewJobRepository reviewJobRepository;

    /**
     * Per-day review counts, token totals, and estimated cost over the last
     * {@code days} days. Days with no activity are simply omitted —
     * the frontend fills them in so the chart's x-axis stays continuous.
     */
    @GetMapping("/daily")
    public List<DailyMetricDto> daily(
            @RequestParam(name = "days", defaultValue = "" + DEFAULT_DAYS) int days) {
        int clamped = Math.min(MAX_DAYS, Math.max(MIN_DAYS, days));
        OffsetDateTime since =
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(clamped).withNano(0);
        return reviewJobRepository.findDailyMetricsSince(since).stream()
                .map(DailyMetricDto::from)
                .toList();
    }
}

package com.codereview.agent.api;

import com.codereview.agent.api.dto.ReviewSummaryDto;
import com.codereview.agent.persistence.repository.ReviewJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only public endpoints powering the operator dashboard.
 *
 * <p>Kept under {@code /api/public/*} on purpose: no auth, no writes,
 * safe to expose to a static SPA on Vercel. CORS is configured in
 * {@code WebConfig}.
 */
@RestController
@RequestMapping("/api/public/reviews")
@RequiredArgsConstructor
public class PublicReviewsController {

    private final ReviewJobRepository reviewJobRepository;

    /**
     * Last 50 review jobs, newest first. Day 2 keeps it deliberately
     * small — no pagination, no filters. Day 3 will add the detail
     * endpoint at {@code /api/public/reviews/{id}}.
     */
    @GetMapping
    public List<ReviewSummaryDto> latest() {
        return reviewJobRepository.findLatestSummaries().stream()
                .map(ReviewSummaryDto::from)
                .toList();
    }
}

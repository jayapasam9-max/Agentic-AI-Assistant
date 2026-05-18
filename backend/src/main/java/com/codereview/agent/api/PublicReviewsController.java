package com.codereview.agent.api;

import com.codereview.agent.api.dto.FindingDto;
import com.codereview.agent.api.dto.ReviewDetailDto;
import com.codereview.agent.api.dto.ReviewSummaryDto;
import com.codereview.agent.api.dto.ToolCallDto;
import com.codereview.agent.persistence.entity.ReviewJob;
import com.codereview.agent.persistence.repository.ReviewFindingRepository;
import com.codereview.agent.persistence.repository.ReviewJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
    private final ReviewFindingRepository reviewFindingRepository;

    /**
     * Last 50 review jobs, newest first. Deliberately small —
     * no pagination, no filters.
     */
    @GetMapping
    public List<ReviewSummaryDto> latest() {
        return reviewJobRepository.findLatestSummaries().stream()
                .map(ReviewSummaryDto::from)
                .toList();
    }

    /**
     * Detail view for a single review: summary + findings + tool-call timeline.
     *
     * <p>The live event stream lives at
     * {@code GET /api/public/reviews/{id}/stream} (SSE).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewDetailDto> detail(@PathVariable UUID id) {
        return reviewJobRepository.findSummaryById(id)
                .map(row -> {
                    ReviewSummaryDto summary = ReviewSummaryDto.from(row);

                    // errorMessage isn't in the summary projection — fetch the
                    // entity for that single field. Cheap, primary-key lookup.
                    String errorMessage = reviewJobRepository.findById(id)
                            .map(ReviewJob::getErrorMessage)
                            .orElse(null);

                    List<FindingDto> findings = reviewFindingRepository.findByJobId(id).stream()
                            .map(FindingDto::from)
                            .toList();

                    List<ToolCallDto> toolCalls = reviewJobRepository.findToolCallsByJobId(id).stream()
                            .map(ToolCallDto::from)
                            .toList();

                    return ResponseEntity.ok(new ReviewDetailDto(summary, errorMessage, findings, toolCalls));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

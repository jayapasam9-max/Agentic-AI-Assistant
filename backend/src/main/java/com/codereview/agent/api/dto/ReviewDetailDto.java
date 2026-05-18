package com.codereview.agent.api.dto;

import java.util.List;

/**
 * Composite response for {@code GET /api/public/reviews/{id}}.
 *
 * <p>Combines the same summary fields shown in the list view with
 * the agent's findings and tool-call timeline so the detail page can
 * render the whole picture from a single request.
 */
public record ReviewDetailDto(
        ReviewSummaryDto summary,
        String errorMessage,
        List<FindingDto> findings,
        List<ToolCallDto> toolCalls) {}

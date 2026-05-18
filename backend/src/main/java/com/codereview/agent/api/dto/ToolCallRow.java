package com.codereview.agent.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Spring Data interface projection for a single row of the agent's
 * tool-call timeline on the review detail page.
 *
 * <p>Sourced from {@code agent_tool_calls} via a native query.
 * The JSONB input/output columns are intentionally not exposed here —
 * they can be large and aren't shown in the timeline.
 */
public interface ToolCallRow {
    UUID getId();

    Integer getIteration();

    String getToolName();

    Integer getDurationMs();

    Boolean getSuccess();

    OffsetDateTime getCreatedAt();
}

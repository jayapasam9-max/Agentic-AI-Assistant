package com.codereview.agent.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Public JSON shape for a single agent tool call. */
public record ToolCallDto(
        UUID id,
        Integer iteration,
        String toolName,
        Integer durationMs,
        Boolean success,
        OffsetDateTime createdAt) {

    public static ToolCallDto from(ToolCallRow row) {
        return new ToolCallDto(
                row.getId(),
                row.getIteration(),
                row.getToolName(),
                row.getDurationMs(),
                row.getSuccess(),
                row.getCreatedAt());
    }
}

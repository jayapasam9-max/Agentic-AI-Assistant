package com.codereview.agent.api.dto;

import com.codereview.agent.persistence.entity.ReviewFinding;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Public JSON shape for a single agent finding. */
public record FindingDto(
        UUID id,
        String filePath,
        Integer lineNumber,
        String severity,
        String category,
        String message,
        String suggestedFix,
        Boolean postedToGithub,
        OffsetDateTime createdAt) {

    public static FindingDto from(ReviewFinding f) {
        return new FindingDto(
                f.getId(),
                f.getFilePath(),
                f.getLineNumber(),
                f.getSeverity() == null ? null : f.getSeverity().name(),
                f.getCategory() == null ? null : f.getCategory().name(),
                f.getMessage(),
                f.getSuggestedFix(),
                f.getPostedToGithub(),
                f.getCreatedAt());
    }
}

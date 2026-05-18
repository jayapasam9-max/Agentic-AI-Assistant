package com.codereview.agent.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Spring Data interface projection for the public reviews list.
 *
 * <p>Used by {@code ReviewJobRepository.findLatestSummaries()} so we can return
 * a JOIN of {@code review_jobs} and {@code repositories} without adding a JPA
 * entity for {@code repositories} (which would risk failing
 * {@code spring.jpa.hibernate.ddl-auto: validate}).
 */
public interface ReviewSummaryRow {
    UUID getId();

    String getRepoFullName();

    Integer getPrNumber();

    String getHeadSha();

    String getStatus();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getStartedAt();

    OffsetDateTime getCompletedAt();

    Integer getTokensInput();

    Integer getTokensOutput();
}

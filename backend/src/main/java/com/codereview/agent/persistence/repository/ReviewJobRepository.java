package com.codereview.agent.persistence.repository;

import com.codereview.agent.api.dto.ReviewSummaryRow;
import com.codereview.agent.api.dto.ToolCallRow;
import com.codereview.agent.persistence.entity.ReviewJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewJobRepository extends JpaRepository<ReviewJob, UUID> {
    List<ReviewJob> findByRepositoryIdAndPrNumberOrderByCreatedAtDesc(UUID repositoryId, Integer prNumber);

    List<ReviewJob> findByStatus(ReviewJob.Status status);

    /**
     * Latest 50 review jobs joined with their repository's github_full_name,
     * for the public dashboard at GET /api/public/reviews.
     *
     * <p>Native query with an interface projection — see
     * {@link ReviewSummaryRow} — so we don't need a JPA entity for
     * the {@code repositories} table.
     */
    @Query(
            value = """
                    SELECT j.id              AS id,
                           r.github_full_name AS repoFullName,
                           j.pr_number       AS prNumber,
                           j.head_sha        AS headSha,
                           j.status          AS status,
                           j.created_at      AS createdAt,
                           j.started_at      AS startedAt,
                           j.completed_at    AS completedAt,
                           j.tokens_input    AS tokensInput,
                           j.tokens_output   AS tokensOutput
                    FROM review_jobs j
                    JOIN repositories r ON r.id = j.repository_id
                    ORDER BY j.created_at DESC
                    LIMIT 50
                    """,
            nativeQuery = true)
    List<ReviewSummaryRow> findLatestSummaries();

    /**
     * Same projection as {@link #findLatestSummaries()} but for a single
     * job id, used by the detail page.
     */
    @Query(
            value = """
                    SELECT j.id              AS id,
                           r.github_full_name AS repoFullName,
                           j.pr_number       AS prNumber,
                           j.head_sha        AS headSha,
                           j.status          AS status,
                           j.created_at      AS createdAt,
                           j.started_at      AS startedAt,
                           j.completed_at    AS completedAt,
                           j.tokens_input    AS tokensInput,
                           j.tokens_output   AS tokensOutput
                    FROM review_jobs j
                    JOIN repositories r ON r.id = j.repository_id
                    WHERE j.id = :id
                    """,
            nativeQuery = true)
    Optional<ReviewSummaryRow> findSummaryById(@Param("id") UUID id);

    /**
     * All tool calls for one job, oldest first — matches the order the agent
     * executed them, which is what we want to render in the timeline.
     */
    @Query(
            value = """
                    SELECT id,
                           iteration,
                           tool_name   AS toolName,
                           duration_ms AS durationMs,
                           success,
                           created_at  AS createdAt
                    FROM agent_tool_calls
                    WHERE job_id = :jobId
                    ORDER BY created_at ASC
                    """,
            nativeQuery = true)
    List<ToolCallRow> findToolCallsByJobId(@Param("jobId") UUID jobId);
}

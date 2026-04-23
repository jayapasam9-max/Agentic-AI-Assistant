package com.codereview.agent.persistence.repository;

import com.codereview.agent.persistence.entity.ReviewJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewJobRepository extends JpaRepository<ReviewJob, UUID> {
    List<ReviewJob> findByRepositoryIdAndPrNumberOrderByCreatedAtDesc(UUID repositoryId, Integer prNumber);
    List<ReviewJob> findByStatus(ReviewJob.Status status);
}

package com.codereview.agent.persistence.repository;

import com.codereview.agent.persistence.entity.ReviewFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, UUID> {
    List<ReviewFinding> findByJobId(UUID jobId);
}

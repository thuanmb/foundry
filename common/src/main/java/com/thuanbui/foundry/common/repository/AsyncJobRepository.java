package com.thuanbui.foundry.common.repository;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, UUID> {

    @Query(value = """
            SELECT * FROM async_jobs
            WHERE status = 'PENDING' AND deleted = FALSE
            ORDER BY priority DESC, created_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<AsyncJob> claimNextJob();

    Optional<AsyncJob> findByParametersHashAndStatusInAndDeletedFalse(
            String parametersHash, List<JobStatus> statuses);

    long countByStatusAndDeletedFalse(JobStatus status);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE async_jobs
            SET status = 'PENDING', message = 'Recovered from stale state', updated_at = CURRENT_TIMESTAMP
            WHERE status = 'RUNNING' AND updated_at < :staleThreshold AND deleted = FALSE
            """, nativeQuery = true)
    int resetStaleJobs(@Param("staleThreshold") Instant staleThreshold);

    @Modifying
    @Query(value = """
            UPDATE async_jobs
            SET updated_at = CURRENT_TIMESTAMP
            WHERE id = :jobId AND status = 'RUNNING'
            """, nativeQuery = true)
    int updateHeartbeat(@Param("jobId") UUID jobId);
}

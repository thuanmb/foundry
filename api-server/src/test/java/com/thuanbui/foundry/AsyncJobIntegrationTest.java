package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.model.enums.JobType;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.services.AsyncJobService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class AsyncJobIntegrationTest {

    private static final String OWNER = "anon:test-owner-hash";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AsyncJobService asyncJobService;

    @Autowired
    private AsyncJobRepository asyncJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        asyncJobRepository.deleteAll();
    }

    @Test
    void shouldCreateJob() {
        Map<String, Object> params = Map.of("message", "hello", "delaySec", 1);
        AsyncJob job = asyncJobService.createJob(JobType.ECHO, "Test Job", params, OWNER, 1, 0, 3);

        assertNotNull(job.getId());
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(JobType.ECHO, job.getJobType());
        assertEquals(64, job.getParametersHash().length());
    }

    @Test
    void shouldDeduplicateByParametersHash() {
        Map<String, Object> params = Map.of("message", "hello", "delaySec", 1);
        AsyncJob job1 = asyncJobService.createJob(JobType.ECHO, "First", params, OWNER, 1, 0, 3);
        AsyncJob job2 = asyncJobService.createJob(JobType.ECHO, "Second", params, OWNER, 1, 0, 3);

        assertEquals(job1.getId(), job2.getId(), "Should return existing job instead of creating duplicate");
    }

    @Test
    void shouldNotDeduplicateAcrossOwners() {
        Map<String, Object> params = Map.of("message", "shared");
        AsyncJob job1 = asyncJobService.createJob(JobType.ECHO, "A", params, OWNER, 1, 0, 3);
        AsyncJob job2 = asyncJobService.createJob(JobType.ECHO, "B", params, "anon:other", 1, 0, 3);

        assertNotEquals(job1.getId(), job2.getId());
        assertEquals(OWNER, asyncJobRepository.findById(job1.getId()).orElseThrow().getOwnerKey());
    }

    @Test
    void getOwnedJob_shouldEnforceOwnership() {
        AsyncJob job = asyncJobService.createJob(JobType.ECHO, "Owned", Map.of("message", "x"), OWNER, 1, 0, 3);

        assertEquals(job.getId(), asyncJobService.getOwnedJob(job.getId(), OWNER).getId());
        assertThrows(AsyncJobService.JobAccessDeniedException.class,
                () -> asyncJobService.getOwnedJob(job.getId(), "anon:someone-else"));
        assertThrows(AsyncJobService.JobNotFoundException.class,
                () -> asyncJobService.getOwnedJob(UUID.randomUUID(), OWNER));
    }

    @Test
    void shouldUpdateJobStatus() {
        Map<String, Object> params = Map.of("message", "m1990");
        AsyncJob job = asyncJobService.createJob(JobType.ECHO, "Test", params, OWNER, 1, 0, 3);

        asyncJobService.updateStatus(job.getId(), JobStatus.RUNNING, "Processing");
        AsyncJob updated = asyncJobService.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.RUNNING, updated.getStatus());
        assertNotNull(updated.getStartedAt());

        asyncJobService.updateStatus(job.getId(), JobStatus.COMPLETED, "Done");
        updated = asyncJobService.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.COMPLETED, updated.getStatus());
        assertNotNull(updated.getCompletedAt());
    }

    @Test
    void shouldClaimNextJobWithSkipLocked() {
        Map<String, Object> params1 = Map.of("message", "m1990");
        Map<String, Object> params2 = Map.of("message", "m1991");

        asyncJobService.createJob(JobType.ECHO, "Job 1", params1, OWNER, 1, 2, 3);
        asyncJobService.createJob(JobType.ECHO, "Job 2", params2, OWNER, 1, 1, 3);

        // Higher priority job should be claimed first
        var claimed = asyncJobRepository.claimNextJob();
        assertTrue(claimed.isPresent());
        assertEquals("Job 1", claimed.get().getJobName());
    }

    @Test
    void shouldResetStaleJobs() {
        Map<String, Object> params = Map.of("message", "m1990");
        AsyncJob job = asyncJobService.createJob(JobType.ECHO, "Stale", params, OWNER, 1, 0, 3);

        // Simulate running but stale
        asyncJobService.updateStatus(job.getId(), JobStatus.RUNNING, "Processing");

        // Temporarily disable trigger to set updated_at in the past
        jdbcTemplate.execute("ALTER TABLE async_jobs DISABLE TRIGGER trg_async_jobs_updated_at");
        try {
            jdbcTemplate.update("UPDATE async_jobs SET updated_at = ? WHERE id = ?",
                    java.sql.Timestamp.from(Instant.now().minusSeconds(600)), job.getId());
        } finally {
            jdbcTemplate.execute("ALTER TABLE async_jobs ENABLE TRIGGER trg_async_jobs_updated_at");
        }

        int reset = asyncJobService.resetStaleJobs();
        assertTrue(reset > 0, "Should reset stale jobs");

        AsyncJob resetJob = asyncJobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.PENDING, resetJob.getStatus());
    }

    @Test
    void shouldFindActiveByHash() {
        Map<String, Object> params = Map.of("message", "active");
        AsyncJob job = asyncJobService.createJob(JobType.ECHO, "Active", params, OWNER, 1, 0, 3);

        var found = asyncJobService.findActiveByHash(job.getParametersHash());
        assertTrue(found.isPresent());
        assertEquals(job.getId(), found.get().getId());
    }
}

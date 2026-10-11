package com.thuanbui.foundry.worker.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.model.enums.JobType;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.worker.listener.PostgresJobListener;

import jakarta.persistence.EntityManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class StaleJobRecoveryTaskTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private StaleJobRecoveryTask staleJobRecoveryTask;

    @Autowired
    private AsyncJobRepository asyncJobRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Disable the background listener so it does not claim the PENDING fixture
    @MockBean
    private PostgresJobListener postgresJobListener;

    @BeforeEach
    void setUp() {
        asyncJobRepository.deleteAll();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        asyncJobRepository.deleteAll();
        entityManager.clear();
    }

    @Test
    void shouldRecoverStaleJobsToPending() {
        AsyncJob staleJob = createRunningJob();
        staleJob.setUpdatedAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        asyncJobRepository.saveAndFlush(staleJob);

        int recovered = staleJobRecoveryTask.recoverStaleJobs();
        assertEquals(1, recovered);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM async_jobs WHERE id = ?", String.class, staleJob.getId());
        assertEquals("PENDING", status);

        String message = jdbcTemplate.queryForObject(
                "SELECT message FROM async_jobs WHERE id = ?", String.class, staleJob.getId());
        assertEquals("Recovered from stale state", message);
    }

    @Test
    void shouldNotRecoverActiveJobs() {
        AsyncJob activeJob = createRunningJob();
        activeJob.setUpdatedAt(Instant.now().minusSeconds(30));
        asyncJobRepository.saveAndFlush(activeJob);

        int recovered = staleJobRecoveryTask.recoverStaleJobs();
        assertEquals(0, recovered);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM async_jobs WHERE id = ?", String.class, activeJob.getId());
        assertEquals("RUNNING", status);
    }

    @Test
    void shouldOnlyRecoverRunningJobs() {
        AsyncJob pendingJob = new AsyncJob();
        pendingJob.setId(UUID.randomUUID());
        pendingJob.setJobType(JobType.ECHO);
        pendingJob.setOwnerKey("user:" + UUID.randomUUID());
        pendingJob.setStatus(JobStatus.PENDING);
        pendingJob.setPriority(0);
        pendingJob.setParameters(Map.of("test", "test"));
        pendingJob.setParametersHash("hash-pending");
        pendingJob.setUpdatedAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        asyncJobRepository.saveAndFlush(pendingJob);

        int recovered = staleJobRecoveryTask.recoverStaleJobs();
        assertEquals(0, recovered);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM async_jobs WHERE id = ?", String.class, pendingJob.getId());
        assertEquals("PENDING", status);
    }

    @Test
    void shouldNotRecoverDeletedJobs() {
        AsyncJob deletedJob = createRunningJob();
        deletedJob.setDeleted(true);
        deletedJob.setUpdatedAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        asyncJobRepository.saveAndFlush(deletedJob);

        int recovered = staleJobRecoveryTask.recoverStaleJobs();
        assertEquals(0, recovered);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM async_jobs WHERE id = ?", String.class, deletedJob.getId());
        assertEquals("RUNNING", status);
    }

    private AsyncJob createRunningJob() {
        AsyncJob job = new AsyncJob();
        job.setId(UUID.randomUUID());
        job.setJobType(JobType.ECHO);
        job.setOwnerKey("user:" + UUID.randomUUID());
        job.setStatus(JobStatus.RUNNING);
        job.setPriority(0);
        job.setParameters(Map.of("test", "test"));
        job.setParametersHash("hash-" + UUID.randomUUID());
        job.setStartedAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        job.setUpdatedAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        return asyncJobRepository.saveAndFlush(job);
    }
}

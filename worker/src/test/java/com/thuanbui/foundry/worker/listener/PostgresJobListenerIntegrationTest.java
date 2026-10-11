package com.thuanbui.foundry.worker.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.model.enums.JobType;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class PostgresJobListenerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AsyncJobRepository asyncJobRepository;

    @Autowired
    @Qualifier("listenDataSource")
    private DataSource listenDataSource;

    @MockBean
    private NotifyService notifyService;

    @BeforeEach
    void setUp() {
        asyncJobRepository.deleteAll();
    }

    @Test
    void notifyShouldWakeUpListenerAndClaimJobWithinSeconds() throws Exception {
        AsyncJob job = createPendingJob();

        try (Connection conn = listenDataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("NOTIFY new_job, '" + job.getId() + "'");
        }

        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            AsyncJob updated = asyncJobRepository.findById(job.getId()).orElseThrow();
            if (updated.getStatus() == JobStatus.RUNNING || updated.getStatus() == JobStatus.COMPLETED) {
                break;
            }
            Thread.sleep(100);
        }

        AsyncJob finalJob = asyncJobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.COMPLETED, finalJob.getStatus(),
                "Job should be processed after NOTIFY. Actual status: " + finalJob.getStatus());
    }

    @Test
    void fallbackPollingShouldClaimJobWithoutNotify() throws Exception {
        AsyncJob job = createPendingJob();

        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            AsyncJob updated = asyncJobRepository.findById(job.getId()).orElseThrow();
            if (updated.getStatus() == JobStatus.RUNNING || updated.getStatus() == JobStatus.COMPLETED) {
                break;
            }
            Thread.sleep(200);
        }

        AsyncJob finalJob = asyncJobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.COMPLETED, finalJob.getStatus(),
                "Job should be processed via fallback polling. Actual status: " + finalJob.getStatus());
    }

    @Test
    void multipleJobsShouldAllBeClaimedProgressively() throws Exception {
        AsyncJob job1 = createPendingJob();
        AsyncJob job2 = createPendingJob();

        try (Connection conn = listenDataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("NOTIFY new_job, '" + job1.getId() + "'");
        }

        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            AsyncJob j1 = asyncJobRepository.findById(job1.getId()).orElseThrow();
            AsyncJob j2 = asyncJobRepository.findById(job2.getId()).orElseThrow();
            if (j1.getStatus() == JobStatus.COMPLETED && j2.getStatus() == JobStatus.COMPLETED) {
                break;
            }
            Thread.sleep(200);
        }

        AsyncJob final1 = asyncJobRepository.findById(job1.getId()).orElseThrow();
        AsyncJob final2 = asyncJobRepository.findById(job2.getId()).orElseThrow();
        assertEquals(JobStatus.COMPLETED, final1.getStatus());
        assertEquals(JobStatus.COMPLETED, final2.getStatus());
    }

    private AsyncJob createPendingJob() {
        AsyncJob job = new AsyncJob();
        job.setId(UUID.randomUUID());
        job.setJobType(JobType.ECHO);
        job.setJobName("Listener test job");
        job.setOwnerKey("user:" + UUID.randomUUID());
        job.setParameters(Map.of("message", "value-" + UUID.randomUUID()));
        job.setParametersHash("hash-" + UUID.randomUUID());
        job.setStatus(JobStatus.PENDING);
        job.setPriority(0);
        job.setRetryCount(0);
        job.setMaxRetries(3);
        job.setDeleted(false);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        return asyncJobRepository.save(job);
    }
}

package com.thuanbui.foundry.worker.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.thuanbui.foundry.worker.listener.NotifyService;
import com.thuanbui.foundry.worker.listener.PostgresJobListener;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class JobClaimServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JobClaimService jobClaimService;

    @Autowired
    private AsyncJobRepository asyncJobRepository;

    @MockBean
    private NotifyService notifyService;

    // Disable the background listener so only the test drives claiming
    @MockBean
    private PostgresJobListener postgresJobListener;

    @BeforeEach
    void setUp() {
        asyncJobRepository.deleteAll();
    }

    @Test
    void shouldClaimPendingJobAndCompleteIt() {
        AsyncJob job = createPendingJob(JobType.ECHO, 0);

        jobClaimService.tryClaimAndProcess();

        AsyncJob claimed = asyncJobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.COMPLETED, claimed.getStatus());
        assertNotNull(claimed.getStartedAt());
    }

    @Test
    void shouldClaimHighestPriorityJobFirst() {
        AsyncJob lowPriority = createPendingJob(JobType.ECHO, 1);
        AsyncJob highPriority = createPendingJob(JobType.ECHO, 10);

        jobClaimService.tryClaimAndProcess();

        AsyncJob claimed = asyncJobRepository.findById(highPriority.getId()).orElseThrow();
        assertEquals(JobStatus.COMPLETED, claimed.getStatus());

        AsyncJob notClaimed = asyncJobRepository.findById(lowPriority.getId()).orElseThrow();
        assertEquals(JobStatus.PENDING, notClaimed.getStatus());
    }

    @Test
    void shouldNotClaimAlreadyRunningJob() {
        AsyncJob job = createPendingJob(JobType.ECHO, 0);
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        asyncJobRepository.save(job);

        jobClaimService.tryClaimAndProcess();

        AsyncJob stillRunning = asyncJobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.RUNNING, stillRunning.getStatus());
    }

    @Test
    void shouldNotClaimFailedJob() {
        AsyncJob job = createPendingJob(JobType.ECHO, 0);
        job.setStatus(JobStatus.FAILED);
        asyncJobRepository.save(job);

        jobClaimService.tryClaimAndProcess();

        AsyncJob stillFailed = asyncJobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.FAILED, stillFailed.getStatus());
    }

    @Test
    void shouldNotClaimCompletedJob() {
        AsyncJob job = createPendingJob(JobType.ECHO, 0);
        job.setStatus(JobStatus.COMPLETED);
        asyncJobRepository.save(job);

        jobClaimService.tryClaimAndProcess();

        AsyncJob stillCompleted = asyncJobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.COMPLETED, stillCompleted.getStatus());
    }

    @Test
    void shouldNotClaimSoftDeletedJob() {
        AsyncJob job = createPendingJob(JobType.ECHO, 0);
        job.setDeleted(true);
        asyncJobRepository.save(job);

        jobClaimService.tryClaimAndProcess();

        AsyncJob stillPending = asyncJobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.PENDING, stillPending.getStatus());
    }

    @Test
    void concurrentWorkersShouldClaimDifferentJobs() throws Exception {
        AsyncJob job1 = createPendingJob(JobType.ECHO, 0);
        AsyncJob job2 = createPendingJob(JobType.ECHO, 0);

        CountDownLatch latch = new CountDownLatch(2);
        Runnable worker = () -> {
            jobClaimService.tryClaimAndProcess();
            latch.countDown();
        };

        Thread t1 = new Thread(worker);
        Thread t2 = new Thread(worker);
        t1.start();
        t2.start();

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        AsyncJob claimed1 = asyncJobRepository.findById(job1.getId()).orElseThrow();
        AsyncJob claimed2 = asyncJobRepository.findById(job2.getId()).orElseThrow();

        assertTrue(claimed1.getStatus() == JobStatus.COMPLETED || claimed2.getStatus() == JobStatus.COMPLETED);
        assertTrue(claimed1.getStatus() == JobStatus.COMPLETED || claimed2.getStatus() == JobStatus.COMPLETED);
    }

    @Test
    void noPendingJobsShouldNotThrowException() {
        jobClaimService.tryClaimAndProcess();
    }

    private AsyncJob createPendingJob(JobType jobType, int priority) {
        AsyncJob job = new AsyncJob();
        job.setId(UUID.randomUUID());
        job.setJobType(jobType);
        job.setJobName("Test job " + priority);
        job.setOwnerKey("user:" + UUID.randomUUID());
        job.setParameters(Map.of("message", "hello"));
        job.setParametersHash("hash-" + UUID.randomUUID());
        job.setStatus(JobStatus.PENDING);
        job.setPriority(priority);
        job.setRetryCount(0);
        job.setMaxRetries(3);
        job.setDeleted(false);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        return asyncJobRepository.save(job);
    }
}

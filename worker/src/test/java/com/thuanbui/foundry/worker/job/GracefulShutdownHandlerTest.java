package com.thuanbui.foundry.worker.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.model.enums.JobType;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.worker.config.WorkerProperties;

@ExtendWith(MockitoExtension.class)
class GracefulShutdownHandlerTest {

    @Mock
    private AsyncJobRepository asyncJobRepository;

    private GracefulShutdownHandler handler;

    @BeforeEach
    void setUp() {
        WorkerProperties properties = new WorkerProperties(
                new WorkerProperties.Job(3, 30, 5, 60, 10),
                new WorkerProperties.Listen("new_job", "job_completed", 30),
                new WorkerProperties.Llm("gpt-4.1-mini", 120, 1000, 0.7),
                new WorkerProperties.Echo(false, 30));
        handler = new GracefulShutdownHandler(asyncJobRepository, properties);
    }

    @Test
    void shouldShutdownImmediatelyWhenNoActiveJob() {
        assertFalse(handler.isShuttingDown());

        handler.onShutdown();

        assertTrue(handler.isShuttingDown());
    }

    @Test
    void shouldReleaseJobWhenRemainingTimeExceedsThreshold() {
        AsyncJob job = createJob(60);
        job.setStartedAt(Instant.now().minusSeconds(10)); // 50s remaining > 10s threshold

        handler.jobStarted(job);
        handler.onShutdown();

        ArgumentCaptor<AsyncJob> captor = ArgumentCaptor.forClass(AsyncJob.class);
        verify(asyncJobRepository).save(captor.capture());
        AsyncJob saved = captor.getValue();
        assertEquals(JobStatus.PENDING, saved.getStatus());
        assertNull(saved.getStartedAt());
        assertEquals("Released by worker shutdown", saved.getMessage());
        assertTrue(handler.isCurrentJobReleased());
    }

    @Test
    void shouldFinishJobWhenRemainingTimeWithinThreshold() throws InterruptedException {
        AsyncJob job = createJob(60);
        job.setStartedAt(Instant.now().minusSeconds(55)); // 5s remaining <= 10s threshold

        handler.jobStarted(job);

        // Simulate job completing in another thread
        Thread completer = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            handler.jobCompleted();
        });
        completer.start();

        handler.onShutdown();

        completer.join(5000);
        assertTrue(handler.isShuttingDown());
    }

    @Test
    void shouldUseDefaultEstimateWhenNotSet() {
        AsyncJob job = createJob(null);
        job.setStartedAt(Instant.now().minusSeconds(100)); // 20s remaining with default 120s

        handler.jobStarted(job);
        handler.onShutdown();

        // 20s > 10s threshold, should release
        ArgumentCaptor<AsyncJob> captor = ArgumentCaptor.forClass(AsyncJob.class);
        verify(asyncJobRepository).save(captor.capture());
        assertEquals(JobStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    void shouldClearJobOnCompletion() {
        AsyncJob job = createJob(60);
        handler.jobStarted(job);
        handler.jobCompleted();

        // After completion, no job should be active
        assertFalse(handler.isShuttingDown());
        // Starting shutdown should be immediate (no active job)
        handler.onShutdown();
        assertTrue(handler.isShuttingDown());
    }

    @Test
    void shouldSetShuttingDownFlag() {
        handler.onShutdown();
        assertTrue(handler.isShuttingDown());
    }

    private AsyncJob createJob(Integer estimatedDurationSec) {
        AsyncJob job = new AsyncJob();
        job.setId(UUID.randomUUID());
        job.setJobType(JobType.ECHO);
        job.setOwnerKey("user:" + UUID.randomUUID());
        job.setParametersHash("hash");
        job.setStatus(JobStatus.RUNNING);
        job.setEstimatedDurationSec(estimatedDurationSec);
        job.setStartedAt(Instant.now());
        return job;
    }
}

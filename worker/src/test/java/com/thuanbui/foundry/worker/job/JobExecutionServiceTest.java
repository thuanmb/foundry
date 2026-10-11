package com.thuanbui.foundry.worker.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.model.enums.JobType;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.worker.config.WorkerProperties;
import com.thuanbui.foundry.worker.listener.NotifyService;
import com.thuanbui.foundry.worker.llm.LlmAuthException;
import com.thuanbui.foundry.worker.llm.LlmRateLimitException;
import com.thuanbui.foundry.worker.llm.LlmResponseParseException;
import com.thuanbui.foundry.worker.llm.LlmServerException;
import com.thuanbui.foundry.worker.llm.LlmTimeoutException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class JobExecutionServiceTest {

    @Mock
    private AsyncJobRepository asyncJobRepository;

    @Mock
    private JobDispatcher jobDispatcher;

    @Mock
    private HeartbeatScheduler heartbeatScheduler;

    @Mock
    private NotifyService notifyService;

    @Mock
    private GracefulShutdownHandler gracefulShutdownHandler;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private WorkerProperties workerProperties;

    private JobExecutionService executionService;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        executionService = new JobExecutionService(
                asyncJobRepository, jobDispatcher, heartbeatScheduler, notifyService,
                gracefulShutdownHandler, transactionTemplate, new SimpleMeterRegistry(), workerProperties);
        jobId = UUID.randomUUID();

        lenient().when(workerProperties.listen())
                .thenReturn(new WorkerProperties.Listen("new_job", "job_completed", 30));

        lenient().doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<Object> callback = invocation.getArgument(0, Consumer.class);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void shouldCompleteJobAndStoreResult() {
        AsyncJob job = createJob(0);
        Map<String, Object> result = Map.of("echo", "hi", "length", 2);
        when(jobDispatcher.dispatch(job)).thenReturn(result);

        executionService.executeJob(job);

        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertEquals(result, job.getResult());
        assertNotNull(job.getCompletedAt());
        assertNull(job.getMessage());
        verify(asyncJobRepository).save(job);
        verify(heartbeatScheduler).startHeartbeat(jobId);
        verify(heartbeatScheduler).stopHeartbeat(jobId);
        verify(notifyService).notify("job_completed", jobId.toString());
    }

    @Test
    void shouldRetryOnRetryableErrorAndResetToPending() {
        AsyncJob job = createJob(3);
        when(jobDispatcher.dispatch(job)).thenThrow(new LlmTimeoutException("timeout", new RuntimeException()));

        executionService.executeJob(job);

        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(1, job.getRetryCount());
        assertEquals("timeout", job.getMessage());
        verify(notifyService, never()).notify(any(), any());
    }

    @Test
    void shouldFailWhenMaxRetriesExceeded() {
        AsyncJob job = createJob(3);
        job.setRetryCount(3);
        when(jobDispatcher.dispatch(job)).thenThrow(new LlmServerException("server error"));

        executionService.executeJob(job);

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertNotNull(job.getCompletedAt());
        verify(notifyService).notify("job_completed", jobId.toString());
    }

    @Test
    void shouldImmediatelyFailOnNonRetryableError() {
        AsyncJob job = createJob(3);
        when(jobDispatcher.dispatch(job)).thenThrow(new LlmAuthException("invalid key"));

        executionService.executeJob(job);

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals("invalid key", job.getMessage());
        assertEquals(0, job.getRetryCount());
        verify(notifyService).notify("job_completed", jobId.toString());
    }

    @Test
    void shouldFailOnPermanentJobException() {
        AsyncJob job = createJob(3);
        when(jobDispatcher.dispatch(job)).thenThrow(new PermanentJobException("bad params"));

        executionService.executeJob(job);

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals("bad params", job.getMessage());
    }

    @Test
    void shouldRetryOnUnexpectedException() {
        AsyncJob job = createJob(3);
        when(jobDispatcher.dispatch(job)).thenThrow(new RuntimeException("unknown"));

        executionService.executeJob(job);

        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(1, job.getRetryCount());
    }

    @Test
    void shouldTreatRateLimitExceptionAsRetryable() {
        AsyncJob job = createJob(3);
        when(jobDispatcher.dispatch(job)).thenThrow(new LlmRateLimitException("rate limited"));

        executionService.executeJob(job);

        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(1, job.getRetryCount());
    }

    @Test
    void shouldTreatResponseParseExceptionAsRetryable() {
        AsyncJob job = createJob(3);
        when(jobDispatcher.dispatch(job)).thenThrow(new LlmResponseParseException("parse error"));

        executionService.executeJob(job);

        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(1, job.getRetryCount());
    }

    @Test
    void shouldAlwaysStopHeartbeatAndMarkJobDone() {
        AsyncJob job = createJob(0);
        when(jobDispatcher.dispatch(job)).thenThrow(new RuntimeException("boom"));

        executionService.executeJob(job);

        verify(heartbeatScheduler).startHeartbeat(jobId);
        verify(heartbeatScheduler).stopHeartbeat(jobId);
        verify(gracefulShutdownHandler).jobStarted(job);
        verify(gracefulShutdownHandler).jobCompleted();
    }

    private AsyncJob createJob(int maxRetries) {
        AsyncJob job = new AsyncJob();
        job.setId(jobId);
        job.setJobType(JobType.ECHO);
        job.setOwnerKey("user:" + UUID.randomUUID());
        job.setParametersHash("hash-" + jobId);
        job.setStatus(JobStatus.RUNNING);
        job.setMaxRetries(maxRetries);
        job.setRetryCount(0);
        job.setParameters(Map.of("message", "hi"));
        return job;
    }
}

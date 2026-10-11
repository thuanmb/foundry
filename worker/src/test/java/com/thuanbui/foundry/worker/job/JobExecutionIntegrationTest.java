package com.thuanbui.foundry.worker.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
class JobExecutionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JobExecutionService jobExecutionService;

    @Autowired
    private AsyncJobRepository asyncJobRepository;

    @MockBean
    private NotifyService notifyService;

    @MockBean
    private PostgresJobListener postgresJobListener;

    @BeforeEach
    void setUp() {
        asyncJobRepository.deleteAll();
    }

    @Test
    void echoJobShouldCompleteAndStoreResult() {
        AsyncJob job = createJob(Map.of("message", "Hello Foundry", "delaySec", 0));

        jobExecutionService.executeJob(job);

        AsyncJob completed = asyncJobRepository.findById(job.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getResult())
                .containsEntry("echo", "Hello Foundry")
                .containsEntry("length", 13)
                .containsKey("processedAt")
                .doesNotContainKey("llmReply");
        verify(notifyService).notify("job_completed", job.getId().toString());
    }

    @Test
    void echoJobWithInvalidParametersShouldFail() {
        AsyncJob job = createJob(Map.of("delaySec", 0));

        jobExecutionService.executeJob(job);

        AsyncJob failed = asyncJobRepository.findById(job.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(failed.getMessage()).contains("message");
        assertThat(failed.getResult()).isNull();
        verify(notifyService).notify("job_completed", job.getId().toString());
    }

    private AsyncJob createJob(Map<String, Object> params) {
        AsyncJob job = new AsyncJob();
        job.setId(UUID.randomUUID());
        job.setJobType(JobType.ECHO);
        job.setJobName("Echo test job");
        job.setOwnerKey("user:" + UUID.randomUUID());
        job.setParameters(params);
        job.setParametersHash("hash-" + UUID.randomUUID());
        job.setStatus(JobStatus.RUNNING);
        job.setPriority(0);
        job.setRetryCount(0);
        job.setMaxRetries(3);
        job.setDeleted(false);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        job.setStartedAt(Instant.now());
        return asyncJobRepository.save(job);
    }
}

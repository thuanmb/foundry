package com.thuanbui.foundry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.model.enums.JobType;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.config.WorkerProperties;
import com.thuanbui.foundry.security.RequestIdentity;
import com.thuanbui.foundry.services.SseSubscriptionRegistry;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "worker.listen.complete-channel=job_completed",
        "worker.listen.channel=new_job"
})
class JobStreamIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AsyncJobRepository asyncJobRepository;
    @Autowired
    private SseSubscriptionRegistry sseSubscriptionRegistry;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private WorkerProperties workerProperties;

    private static final String TOKEN_HASH = "test-hash-status";

    @AfterEach
    void tearDown() {
        asyncJobRepository.deleteAll();
    }

    private UUID createJob() {
        AsyncJob job = new AsyncJob();
        job.setJobType(JobType.ECHO);
        job.setJobName("echo");
        job.setParameters(Map.of("message", "hello", "delaySec", 0));
        job.setParametersHash("hash-" + UUID.randomUUID());
        job.setOwnerKey("anon:" + TOKEN_HASH);
        job.setStatus(JobStatus.PENDING);
        job.setPriority(0);
        job.setMaxRetries(3);
        job.setRetryCount(0);
        job.setDeleted(false);
        job.setEstimatedDurationSec(60);
        return asyncJobRepository.saveAndFlush(job).getId();
    }

    private void completeJob(UUID jobId, Map<String, Object> result) {
        AsyncJob job = asyncJobRepository.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.COMPLETED);
        job.setResult(result);
        job.setCompletedAt(Instant.now());
        asyncJobRepository.saveAndFlush(job);
    }

    @Test
    void statusEndpoint_shouldReturnPendingJob() throws Exception {
        UUID jobId = createJob();

        MvcResult result = mockMvc.perform(get("/api/v1/jobs/" + jobId + "/status")
                        .requestAttr(RequestIdentity.ANONYMOUS_TOKEN_HASH_ATTR, TOKEN_HASH))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThat(json).contains("\"status\":\"PENDING\"");
        assertThat(json).contains("\"jobId\":\"" + jobId + "\"");
        assertThat(json).contains("\"estimatedWaitSec\":60");
    }

    @Test
    void statusEndpoint_completedJob_shouldReturnResult() throws Exception {
        UUID jobId = createJob();
        completeJob(jobId, Map.of("echo", "Test result", "length", 11));

        MvcResult result = mockMvc.perform(get("/api/v1/jobs/" + jobId + "/status")
                        .requestAttr(RequestIdentity.ANONYMOUS_TOKEN_HASH_ATTR, TOKEN_HASH))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThat(json).contains("\"status\":\"COMPLETED\"");
        assertThat(json).contains("\"echo\":\"Test result\"");
    }

    @Test
    void statusEndpoint_failedJob_shouldReturnError() throws Exception {
        UUID jobId = createJob();
        AsyncJob job = asyncJobRepository.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.FAILED);
        job.setMessage("boom");
        asyncJobRepository.saveAndFlush(job);

        String json = mockMvc.perform(get("/api/v1/jobs/" + jobId + "/status")
                        .requestAttr(RequestIdentity.ANONYMOUS_TOKEN_HASH_ATTR, TOKEN_HASH))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(json).contains("\"status\":\"FAILED\"");
        assertThat(json).contains("\"code\":\"JOB_FAILED\"");
        assertThat(json).contains("\"message\":\"boom\"");
    }

    @Test
    void statusEndpoint_otherOwner_shouldReturn403() throws Exception {
        UUID jobId = createJob();

        mockMvc.perform(get("/api/v1/jobs/" + jobId + "/status")
                        .requestAttr(RequestIdentity.ANONYMOUS_TOKEN_HASH_ATTR, "wrong-hash"))
                .andExpect(status().isForbidden());
    }

    @Test
    void statusEndpoint_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/" + UUID.randomUUID() + "/status")
                        .requestAttr(RequestIdentity.ANONYMOUS_TOKEN_HASH_ATTR, TOKEN_HASH))
                .andExpect(status().isNotFound());
    }

    @Test
    void streamEndpoint_shouldReturnSseStream() throws Exception {
        UUID jobId = createJob();

        MvcResult result = mockMvc.perform(get("/api/v1/jobs/" + jobId + "/stream")
                        .requestAttr(RequestIdentity.ANONYMOUS_TOKEN_HASH_ATTR, TOKEN_HASH)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).contains("text/event-stream");
    }

    @Test
    void streamEndpoint_otherOwner_shouldReturn403() throws Exception {
        UUID jobId = createJob();

        mockMvc.perform(get("/api/v1/jobs/" + jobId + "/stream")
                        .requestAttr(RequestIdentity.ANONYMOUS_TOKEN_HASH_ATTR, "wrong-hash")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isForbidden());
    }

    @Test
    void streamEndpoint_completedJob_shouldReturnImmediateCompletedEvent() throws Exception {
        UUID jobId = createJob();
        completeJob(jobId, Map.of("echo", "done"));

        MvcResult result = mockMvc.perform(get("/api/v1/jobs/" + jobId + "/stream")
                        .requestAttr(RequestIdentity.ANONYMOUS_TOKEN_HASH_ATTR, TOKEN_HASH)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).contains("text/event-stream");
        assertThat(result.getResponse().getContentAsString()).contains("event:completed").contains("done");
    }

    @Test
    void streamEndpoint_completeJobThenNotify_shouldPushToSubscribers() throws Exception {
        UUID jobId = createJob();

        SseEmitter testEmitter = new SseEmitter();
        sseSubscriptionRegistry.subscribe(jobId, testEmitter);

        // Simulate the worker: persist the result then NOTIFY the completion channel
        jdbcTemplate.update(
                "UPDATE async_jobs SET status = 'COMPLETED', result = '{\"echo\":\"e2e\"}'::jsonb, "
                        + "completed_at = NOW() WHERE id = ?",
                jobId);
        jdbcTemplate.execute("NOTIFY " + workerProperties.listen().completeChannel() + ", '" + jobId + "'");

        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline && !sseSubscriptionRegistry.getSubscribers(jobId).isEmpty()) {
            Thread.sleep(100);
        }

        assertThat(sseSubscriptionRegistry.getSubscribers(jobId))
                .withFailMessage("Listener should have removed the emitter after processing the notification")
                .isEmpty();
    }
}

package com.thuanbui.foundry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.model.enums.JobType;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.services.JweTokenService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EchoJobIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }

    private static final String ECHO = "/api/v1/jobs/echo";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AsyncJobRepository asyncJobRepository;
    @Autowired
    private JweTokenService jweTokenService;

    @BeforeEach
    void setUp() {
        asyncJobRepository.deleteAll();
    }

    @Test
    void anonymousCaller_shouldCreateAndReadOwnJob() throws Exception {
        String jwe = jweTokenService.generateAnonymousToken("WEB");

        String body = mockMvc.perform(post(ECHO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\",\"delaySec\":2}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String jobId = objectMapper.readTree(body).get("jobId").asText();
        assertThat(jobId).isNotBlank();

        AsyncJob job = asyncJobRepository.findById(UUID.fromString(jobId)).orElseThrow();
        assertThat(job.getJobType()).isEqualTo(JobType.ECHO);
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getOwnerKey()).isEqualTo("anon:" + jweTokenService.hashToken(jwe));
        assertThat(job.getParameters()).containsEntry("message", "hello").containsEntry("delaySec", 2);

        mockMvc.perform(get("/api/v1/jobs/" + jobId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void ownerKey_shouldStartWithAnonPrefix() throws Exception {
        String jwe = jweTokenService.generateAnonymousToken("MOBILE");
        String jobId = createJob(jwe, "prefix check");

        AsyncJob job = asyncJobRepository.findById(UUID.fromString(jobId)).orElseThrow();
        assertThat(job.getOwnerKey()).startsWith("anon:");
    }

    @Test
    void differentAnonymousToken_shouldGet403() throws Exception {
        String jobId = createJob(jweTokenService.generateAnonymousToken("WEB"), "mine");
        String otherJwe = jweTokenService.generateAnonymousToken("WEB");

        mockMvc.perform(get("/api/v1/jobs/" + jobId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherJwe))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void emptyMessage_shouldReturn400() throws Exception {
        String jwe = jweTokenService.generateAnonymousToken("WEB");

        mockMvc.perform(post(ECHO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(asyncJobRepository.count()).isZero();
    }

    @Test
    void withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post(ECHO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String createJob(String jwe, String message) throws Exception {
        String body = mockMvc.perform(post(ECHO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("message", message))))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("jobId").asText();
    }
}

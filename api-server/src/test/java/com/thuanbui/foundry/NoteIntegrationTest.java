package com.thuanbui.foundry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thuanbui.foundry.common.model.entity.User;
import com.thuanbui.foundry.common.repository.NoteRepository;
import com.thuanbui.foundry.common.repository.UserRepository;
import com.thuanbui.foundry.model.dto.NoteRequest;
import com.thuanbui.foundry.services.JweTokenService;
import com.thuanbui.foundry.services.JwtTokenService;
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
class NoteIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }

    private static final String NOTES = "/api/v1/notes";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private JweTokenService jweTokenService;

    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUp() {
        noteRepository.deleteAll();
        userRepository.deleteAll();
        aliceToken = bearer(jwtTokenService.generateAccessToken(createUser("+84900000001")));
        bobToken = bearer(jwtTokenService.generateAccessToken(createUser("+84900000002")));
    }

    @Test
    void crud_shouldWorkForOwner() throws Exception {
        String created = mockMvc.perform(post(NOTES)
                        .header(HttpHeaders.AUTHORIZATION, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NoteRequest("First", "Hello"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.title").value("First"))
                .andExpect(jsonPath("$.content").value("Hello"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get(NOTES + "/" + id).header(HttpHeaders.AUTHORIZATION, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("First"));

        mockMvc.perform(put(NOTES + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NoteRequest("Updated", "Changed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.content").value("Changed"));

        String list = mockMvc.perform(get(NOTES).header(HttpHeaders.AUTHORIZATION, aliceToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode page = objectMapper.readTree(list);
        assertThat(page.get("totalElements").asLong()).isEqualTo(1);
        assertThat(page.get("page").asInt()).isZero();
        assertThat(page.get("size").asInt()).isEqualTo(20);
        assertThat(page.get("totalPages").asInt()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("title").asText()).isEqualTo("Updated");

        mockMvc.perform(delete(NOTES + "/" + id).header(HttpHeaders.AUTHORIZATION, aliceToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(NOTES + "/" + id).header(HttpHeaders.AUTHORIZATION, aliceToken))
                .andExpect(status().isNotFound());
        assertThat(noteRepository.count()).isZero();
    }

    @Test
    void list_shouldOnlyReturnCallersNotes() throws Exception {
        createNote(aliceToken, "Alice note");
        createNote(bobToken, "Bob note");

        mockMvc.perform(get(NOTES).header(HttpHeaders.AUTHORIZATION, bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Bob note"));
    }

    @Test
    void withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get(NOTES))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post(NOTES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NoteRequest("x", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withAnonymousToken_shouldReturn401() throws Exception {
        String anonToken = bearer(jweTokenService.generateAnonymousToken("WEB"));

        mockMvc.perform(get(NOTES).header(HttpHeaders.AUTHORIZATION, anonToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(NOTES)
                        .header(HttpHeaders.AUTHORIZATION, anonToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NoteRequest("x", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anotherUsersNote_shouldReturn404() throws Exception {
        String id = createNote(aliceToken, "Private");

        mockMvc.perform(get(NOTES + "/" + id).header(HttpHeaders.AUTHORIZATION, bobToken))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(put(NOTES + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NoteRequest("Hijack", null))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(NOTES + "/" + id).header(HttpHeaders.AUTHORIZATION, bobToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(NOTES + "/" + id).header(HttpHeaders.AUTHORIZATION, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Private"));
    }

    @Test
    void invalidBody_shouldReturn400ProblemJson() throws Exception {
        mockMvc.perform(post(NOTES)
                        .header(HttpHeaders.AUTHORIZATION, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post(NOTES)
                        .header(HttpHeaders.AUTHORIZATION, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NoteRequest("x".repeat(201), null))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private UUID createUser(String phone) {
        User user = new User();
        user.setPhoneNumber(phone);
        return userRepository.save(user).getId();
    }

    private String createNote(String token, String title) throws Exception {
        String body = mockMvc.perform(post(NOTES)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NoteRequest(title, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}

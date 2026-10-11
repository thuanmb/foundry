package com.thuanbui.foundry.worker.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    private static final String SYSTEM_KEY = "echo_prompt.system.txt";
    private static final String USER_KEY = "echo_prompt.user.txt";

    @Mock
    private OpenAiConfigService configService;

    private PromptTemplateService service;

    @BeforeEach
    void setUp() {
        service = new PromptTemplateService(configService);
    }

    @Test
    void shouldReturnTemplate() {
        when(configService.getConfig(SYSTEM_KEY)).thenReturn("You are a helpful assistant.");

        assertEquals("You are a helpful assistant.", service.getTemplate(SYSTEM_KEY));
    }

    @Test
    void shouldThrowWhenTemplateMissing() {
        when(configService.getConfig(SYSTEM_KEY)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.getTemplate(SYSTEM_KEY));
    }

    @Test
    void shouldSubstituteVariables() {
        when(configService.getConfig(USER_KEY)).thenReturn("Message: {{message}} / {{ message }} / {{unknown}}");

        String result = service.render(USER_KEY, Map.of("message", "hi $1 \\"));

        assertEquals("Message: hi $1 \\ / hi $1 \\ / {{unknown}}", result);
    }

    @Test
    void shouldNotReExpandSubstitutedValues() {
        when(configService.getConfig(USER_KEY)).thenReturn("{{a}}");

        assertEquals("{{b}}", service.render(USER_KEY, Map.of("a", "{{b}}", "b", "x")));
    }

    @Test
    void bundledEchoPromptsShouldLoadAndRender() {
        OpenAiConfigService realConfig = new OpenAiConfigService(new DefaultResourceLoader(),
                new String[] { "classpath:prompts/" + SYSTEM_KEY, "/nonexistent/dir/" + USER_KEY });
        realConfig.init();
        PromptTemplateService realService = new PromptTemplateService(realConfig);

        assertTrue(!realService.getTemplate(SYSTEM_KEY).isBlank());
        assertTrue(realService.render(USER_KEY, Map.of("message", "ping")).contains("ping"));
    }
}

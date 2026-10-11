package com.thuanbui.foundry.worker.llm;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * Resolves prompt templates loaded by {@link OpenAiConfigService} and
 * substitutes {@code {{var}}} placeholders.
 * Unknown placeholders are left untouched.
 */
@Service
public class PromptTemplateService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");

    private final OpenAiConfigService configService;

    public PromptTemplateService(OpenAiConfigService configService) {
        this.configService = configService;
    }

    public String getTemplate(String name) {
        String template = configService.getConfig(name);
        if (template == null || template.isBlank()) {
            throw new IllegalStateException("Prompt template not found: " + name);
        }
        return template;
    }

    public String render(String name, Map<String, ?> variables) {
        String template = getTemplate(name);
        // Single pass so substituted values containing {{...}} are not re-expanded
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = variables.containsKey(key)
                    ? String.valueOf(variables.get(key))
                    : matcher.group();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}

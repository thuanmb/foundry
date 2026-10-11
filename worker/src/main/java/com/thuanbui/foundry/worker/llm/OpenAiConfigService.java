package com.thuanbui.foundry.worker.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Loads text config files (e.g. prompt templates) listed in
 * {@code config.files.paths}, keyed by file name.
 * Entries may be filesystem paths (e.g. a mounted ConfigMap) or
 * {@code classpath:} locations. A filesystem
 * path that does not exist falls back to the bundled
 * {@code classpath:prompts/<file name>}.
 */
@Service
public class OpenAiConfigService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiConfigService.class);
    private static final String CLASSPATH_FALLBACK_DIR = "classpath:prompts/";

    private final Map<String, String> configContents = new ConcurrentHashMap<>();
    private final ResourceLoader resourceLoader;
    private final String[] configFilePaths;

    public OpenAiConfigService(ResourceLoader resourceLoader,
            @Value("${config.files.paths:}") String[] configFilePaths) {
        this.resourceLoader = resourceLoader;
        this.configFilePaths = configFilePaths;
    }

    @PostConstruct
    public void init() {
        loadAllConfigs();
    }

    public String getConfig(String name) {
        return configContents.get(name);
    }

    public void reloadAll() {
        loadAllConfigs();
    }

    private void loadAllConfigs() {
        configContents.clear();
        for (String rawPath : configFilePaths) {
            String path = rawPath.trim();
            if (path.isEmpty()) {
                continue;
            }
            String name = path.substring(path.lastIndexOf('/') + 1);
            configContents.put(name, load(path, name));
        }
        log.info("Loaded config files: {}", configContents.keySet());
    }

    private String load(String path, String name) {
        try {
            if (path.startsWith("classpath:")) {
                return readResource(path);
            }
            Path filePath = Path.of(path);
            if (Files.exists(filePath)) {
                return Files.readString(filePath);
            }
            log.warn("Config file {} not found, falling back to {}{}", path, CLASSPATH_FALLBACK_DIR, name);
            return readResource(CLASSPATH_FALLBACK_DIR + name);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config from: " + path, e);
        }
    }

    private String readResource(String location) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IOException("Resource not found: " + location);
        }
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

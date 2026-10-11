package com.thuanbui.foundry.worker.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WorkerConfig {

    @Bean
    public ThreadPoolTaskExecutor jobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("job-");
        executor.initialize();
        return executor;
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public ExecutorService listenerExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pg-listener");
            t.setDaemon(true);
            return t;
        });
    }

    @Bean
    public WebClient webClient(OpenAiProperties openAiProperties) {
        return WebClient.builder()
                .baseUrl(openAiProperties.baseUrl())
                .build();
    }
}

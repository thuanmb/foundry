package com.thuanbui.foundry.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.thuanbui.foundry.worker.config.OpenAiProperties;
import com.thuanbui.foundry.worker.config.WorkerProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({ WorkerProperties.class, OpenAiProperties.class })
@EntityScan(basePackages = "com.thuanbui.foundry.common.model.entity")
@EnableJpaRepositories(basePackages = "com.thuanbui.foundry.common.repository")
public class FoundryWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoundryWorkerApplication.class, args);
    }
}

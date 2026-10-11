package com.thuanbui.foundry;

import com.thuanbui.foundry.config.AccountProperties;
import com.thuanbui.foundry.config.AppProperties;
import com.thuanbui.foundry.config.AttestationProperties;
import com.thuanbui.foundry.config.AuthProperties;
import com.thuanbui.foundry.config.CacheProperties;
import com.thuanbui.foundry.config.FeatureProperties;
import com.thuanbui.foundry.config.PollingProperties;
import com.thuanbui.foundry.config.SecurityProperties;
import com.thuanbui.foundry.config.SmsProperties;
import com.thuanbui.foundry.config.SseProperties;
import com.thuanbui.foundry.config.TurnstileProperties;
import com.thuanbui.foundry.config.WorkerProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        AuthProperties.class,
        WorkerProperties.class,
        CacheProperties.class,
        SecurityProperties.class,
        SseProperties.class,
        FeatureProperties.class,
        AccountProperties.class,
        AppProperties.class,
        SmsProperties.class,
        TurnstileProperties.class,
        AttestationProperties.class,
        PollingProperties.class
})
public class FoundryApplication {
    public static void main(String... args) {
        SpringApplication.run(FoundryApplication.class, args);
    }
}

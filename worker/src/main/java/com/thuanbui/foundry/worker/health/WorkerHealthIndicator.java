package com.thuanbui.foundry.worker.health;

import javax.sql.DataSource;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class WorkerHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public WorkerHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (var conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            if (valid) {
                return Health.up()
                    .withDetail("database", "connected")
                    .build();
            }
            return Health.down().withDetail("database", "invalid").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("database", "unreachable").build();
        }
    }
}

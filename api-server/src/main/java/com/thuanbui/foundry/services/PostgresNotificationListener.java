package com.thuanbui.foundry.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Base class for Postgres LISTEN loops with automatic reconnect.
 * Subclasses provide the channel name and the payload handler.
 */
public abstract class PostgresNotificationListener {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Environment environment;
    private final ExecutorService executor;
    private volatile boolean running = true;

    protected PostgresNotificationListener(Environment environment) {
        this.environment = environment;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, getClass().getSimpleName() + "-listener");
            thread.setDaemon(true);
            return thread;
        });
    }

    protected abstract String channel();

    protected abstract void handleNotification(String payload);

    @PostConstruct
    void startListening() {
        executor.submit(() -> {
            while (running) {
                try {
                    listenLoop();
                } catch (Exception e) {
                    log.error("LISTEN loop on channel {} failed, reconnecting in 5s", channel(), e);
                    sleepUnchecked(5000);
                }
            }
        });
    }

    private void listenLoop() throws Exception {
        String url = environment.getRequiredProperty("spring.datasource.url");
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("LISTEN " + channel());
                log.info("Listening on channel: {}", channel());

                while (running && !conn.isClosed()) {
                    org.postgresql.PGConnection pgConn = conn.unwrap(org.postgresql.PGConnection.class);
                    org.postgresql.PGNotification[] notifications = pgConn.getNotifications(1000);
                    if (notifications != null) {
                        for (org.postgresql.PGNotification notification : notifications) {
                            try {
                                handleNotification(notification.getParameter());
                            } catch (Exception e) {
                                log.error("Failed to handle notification on {}: {}", channel(),
                                        notification.getParameter(), e);
                            }
                        }
                    }
                }
            }
        }
    }

    private void sleepUnchecked(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    void shutdown() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

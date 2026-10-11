package com.thuanbui.foundry.worker.listener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.thuanbui.foundry.worker.config.WorkerProperties;
import com.thuanbui.foundry.worker.job.JobClaimService;

@Component
public class PostgresJobListener implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(PostgresJobListener.class);
    private static final long MAX_BACKOFF_MS = 30_000;
    private static final long INITIAL_BACKOFF_MS = 1_000;

    private final DataSource listenDataSource;
    private final JobClaimService jobClaimService;
    private final WorkerProperties workerProperties;
    private volatile boolean running = true;
    private Thread listenerThread;

    public PostgresJobListener(
            @Qualifier("listenDataSource") DataSource listenDataSource,
            JobClaimService jobClaimService,
            WorkerProperties workerProperties) {
        this.listenDataSource = listenDataSource;
        this.jobClaimService = jobClaimService;
        this.workerProperties = workerProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        listenerThread = Thread.ofVirtual().name("pg-listen").start(this::listenLoop);
    }

    private void listenLoop() {
        long backoffMs = INITIAL_BACKOFF_MS;

        while (running) {
            try (Connection conn = listenDataSource.getConnection();
                    Statement stmt = conn.createStatement()) {

                String channel = workerProperties.listen().channel();
                stmt.execute("LISTEN " + channel);
                log.info("Listening on PostgreSQL channel: {}", channel);
                backoffMs = INITIAL_BACKOFF_MS;

                PGConnection pgConn = conn.unwrap(PGConnection.class);
                int timeoutMs = workerProperties.listen().fallbackPollSec() * 1000;

                while (running) {
                    PGNotification[] notifications = pgConn.getNotifications(timeoutMs);

                    if (notifications != null && notifications.length > 0) {
                        for (PGNotification notification : notifications) {
                            log.debug("Received notification: {} = {}",
                                    notification.getName(), notification.getParameter());
                        }
                    }

                    jobClaimService.tryClaimAndProcess();
                }
            } catch (SQLException e) {
                if (!running) {
                    break;
                }
                log.error("LISTEN connection lost, reconnecting in {}ms", backoffMs, e);
                sleepMs(backoffMs);
                backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
            } catch (Exception e) {
                if (!running) {
                    break;
                }
                log.error("Unexpected error in listener loop", e);
                sleepMs(backoffMs);
                backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
            }
        }
    }

    private void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void destroy() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
}

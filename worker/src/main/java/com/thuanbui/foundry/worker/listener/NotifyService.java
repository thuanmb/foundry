package com.thuanbui.foundry.worker.listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class NotifyService {

    private static final Logger log = LoggerFactory.getLogger(NotifyService.class);

    private final DataSource listenDataSource;

    public NotifyService(@Qualifier("listenDataSource") DataSource listenDataSource) {
        this.listenDataSource = listenDataSource;
    }

    public void notify(String channel, String payload) {
        try (Connection conn = listenDataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT pg_notify(?, ?)")) {
            stmt.setString(1, channel);
            stmt.setString(2, payload);
            stmt.execute();
            log.debug("Sent NOTIFY {} with payload {}", channel, payload);
        } catch (SQLException e) {
            log.error("Failed to send NOTIFY {}: {}", channel, e.getMessage());
        }
    }
}

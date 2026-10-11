package com.thuanbui.foundry.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PostgresNotifyService {

    private final JdbcTemplate jdbcTemplate;

    public PostgresNotifyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void notify(String channel, String payload) {
        jdbcTemplate.execute("NOTIFY " + channel + ", '" + payload + "'");
    }
}

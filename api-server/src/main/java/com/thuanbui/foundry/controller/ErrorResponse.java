package com.thuanbui.foundry.controller;

import java.time.Instant;

public record ErrorResponse(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    Instant timestamp
) {
    public ErrorResponse(String type, String title, int status, String detail) {
        this(type, title, status, detail, null, Instant.now());
    }
}

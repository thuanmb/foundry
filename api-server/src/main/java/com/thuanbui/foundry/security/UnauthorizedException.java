package com.thuanbui.foundry.security;

/** Thrown when an endpoint requires an authenticated (JWT) caller. Mapped to 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() {
        super("Authentication required");
    }
}

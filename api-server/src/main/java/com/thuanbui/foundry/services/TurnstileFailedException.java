package com.thuanbui.foundry.services;

public class TurnstileFailedException extends RuntimeException {
    public TurnstileFailedException() {
        super("Turnstile verification failed");
    }
}

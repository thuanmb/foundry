package com.thuanbui.foundry.services;

public class AttestationFailedException extends RuntimeException {
    public AttestationFailedException() {
        super("Device attestation verification failed");
    }
}

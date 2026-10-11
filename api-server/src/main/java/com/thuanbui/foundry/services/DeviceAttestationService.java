package com.thuanbui.foundry.services;

public interface DeviceAttestationService {
    boolean verify(String attestationToken, String platform);
}

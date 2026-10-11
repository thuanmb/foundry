import * as AppIntegrity from '@expo/app-integrity';
import { Platform } from 'react-native';
import Constants from 'expo-constants';
import api from './api';
import { secureStorage } from './secureStorage';

const JWE_KEY = 'jweToken';
const JWE_EXPIRY_KEY = 'jweExpiresAt';
const KEY_ID_KEY = 'attestationKeyId';

export interface AttestationResult {
    token: string;
    expiresAt: string;
}

export function isAttestationSupported(): boolean {
    try {
        return AppIntegrity.isSupported;
    } catch {
        return false;
    }
}


async function getIosAttestation(): Promise<AttestationResult | null> {
    // 1. Get or generate key pair
    let keyId = await secureStorage.getItem(KEY_ID_KEY);

    try {
        if (!keyId) {
            keyId = await AppIntegrity.generateKeyAsync();
            await secureStorage.setItem(KEY_ID_KEY, keyId);
        }
    } catch {
        // Key generation failed — device likely doesn't support attestation
        return null;
    }

    // 2. Get server challenge
    const { data } = await api.get<{ nonce: string }>('/device/challenge');

    // 3. Attest the key
    try {
        const attestation = await AppIntegrity.attestKeyAsync(keyId, data.nonce);

        // 4. Send to backend for verification
        const response = await api.post<{ jweToken: string; expiresAt: string }>(
            '/device/verify',
            { attestationToken: attestation, platform: 'IOS' },
        );

        return { token: response.data.jweToken, expiresAt: response.data.expiresAt };
    } catch {
        // Key might be invalid (e.g., app reinstalled). Clear and let next call retry.
        await secureStorage.removeItem(KEY_ID_KEY);
        return null;
    }
}

async function getAndroidAttestation(): Promise<AttestationResult | null> {
    try {
        const cloudProjectNumber =
            Constants.expoConfig?.extra?.playIntegrityProjectNumber as string | undefined;
        if (!cloudProjectNumber) {
            return null;
        }

        await AppIntegrity.prepareIntegrityTokenProviderAsync(cloudProjectNumber);

        // Get server challenge
        const { data } = await api.get<{ nonce: string }>('/device/challenge');
        const requestHash = await hashString(data.nonce);

        const integrityToken =
            await AppIntegrity.requestIntegrityCheckAsync(requestHash);

        const response = await api.post<{ jweToken: string; expiresAt: string }>(
            '/device/verify',
            { attestationToken: integrityToken, platform: 'ANDROID' },
        );

        return { token: response.data.jweToken, expiresAt: response.data.expiresAt };
    } catch {
        return null;
    }
}

async function hashString(input: string): Promise<string> {
    // Simple hash for Play Integrity requestHash
    // In a production app, use expo-crypto or a native hashing module
    let hash = 0;
    for (let i = 0; i < input.length; i++) {
        const char = input.charCodeAt(i);
        hash = (hash << 5) - hash + char;
        hash |= 0;
    }
    return Math.abs(hash).toString(16);
}

export async function performAttestation(): Promise<AttestationResult | null> {
    const supported = isAttestationSupported();

    if (supported) {
        if (Platform.OS === 'ios') {
            return getIosAttestation();
        } else if (Platform.OS === 'android') {
            return getAndroidAttestation();
        }
    }

    // Fallback for simulators/emulators — backend must have attestation.bypass-enabled=true
    if (__DEV__) {
        console.log('[attestation] Device attestation not supported, using dev bypass');
        return performDevBypass();
    }

    return null;
}

/**
 * Development-only bypass for simulators and emulators.
 * Calls POST /api/v1/device/verify with a placeholder token.
 * The backend must have attestation.bypass-enabled=true to accept this.
 */
async function performDevBypass(): Promise<AttestationResult | null> {
    try {
        const platform = Platform.OS === 'ios' ? 'IOS' : 'ANDROID';
        const response = await api.post<{ jweToken: string; expiresAt: string }>(
            '/device/verify',
            { attestationToken: 'dev-bypass-token', platform },
        );
        console.log('[attestation] Dev bypass succeeded, JWE token obtained');
        return { token: response.data.jweToken, expiresAt: response.data.expiresAt };
    } catch (error) {
        console.warn('[attestation] Dev bypass failed:', error);
        return null;
    }
}

export async function storeJweToken(token: string, expiresAt: string): Promise<void> {
    await secureStorage.setItem(JWE_KEY, token);
    await secureStorage.setItem(JWE_EXPIRY_KEY, expiresAt);
}

export async function getStoredJweToken(): Promise<string | null> {
    const token = await secureStorage.getItem(JWE_KEY);
    const expiry = await secureStorage.getItem(JWE_EXPIRY_KEY);

    if (token && expiry) {
        const expiryDate = new Date(expiry);
        if (expiryDate > new Date()) {
            return token;
        }
        // Token expired — clear it
        await secureStorage.removeItem(JWE_KEY);
        await secureStorage.removeItem(JWE_EXPIRY_KEY);
    }

    return null;
}

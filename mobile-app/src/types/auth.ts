export interface SendOtpRequest {
    phoneNumber: string;
}

export interface SendOtpResponse {
    otpSent: boolean;
    expiresInSec: number;
}

export interface VerifyOtpRequest {
    phoneNumber: string;
    otp: string;
}

export interface UserProfile {
    id: string;
    phoneNumber: string;
    displayName?: string | null;
    createdAt?: string;
}

export interface UpdateProfileRequest {
    displayName: string;
}

export interface AuthTokensResponse {
    accessToken: string;
    refreshToken: string;
    accessExpiresAt: string;
    refreshExpiresAt: string;
    user: UserProfile;
    isNewUser: boolean;
}

export type VerifyOtpResponse = AuthTokensResponse;
export type FirebaseVerifyResponse = AuthTokensResponse;

export interface FirebaseVerifyRequest {
    idToken: string;
}

export interface DeleteAccountRequest {
    otp: string;
}

export interface DeleteAccountResponse {
    success: boolean;
    deletionScheduledAt: string;
}

export interface RefreshRequest {
    refreshToken: string;
}

export interface RefreshResponse {
    accessToken: string;
    refreshToken: string;
    accessExpiresAt: string;
    refreshExpiresAt: string;
}

/** RFC 7807 Problem Details. */
export interface ProblemDetail {
    type?: string;
    title?: string;
    status?: number;
    detail?: string;
    instance?: string;
    code?: string;
    [key: string]: unknown;
}

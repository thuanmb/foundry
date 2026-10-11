import api from './api';
import type {
    SendOtpRequest,
    SendOtpResponse,
    VerifyOtpRequest,
    VerifyOtpResponse,
    RefreshRequest,
    RefreshResponse,
    FirebaseVerifyRequest,
    FirebaseVerifyResponse,
    UpdateProfileRequest,
    UserProfile,
    DeleteAccountRequest,
    DeleteAccountResponse,
} from '@/types/auth';

export async function sendOtp(data: SendOtpRequest): Promise<SendOtpResponse> {
    const { data: response } = await api.post<SendOtpResponse>('/auth/send-otp', data);
    return response;
}

export async function verifyOtp(data: VerifyOtpRequest): Promise<VerifyOtpResponse> {
    const { data: response } = await api.post<VerifyOtpResponse>('/auth/verify-otp', data);
    return response;
}

export async function refreshToken(data: RefreshRequest): Promise<RefreshResponse> {
    const { data: response } = await api.post<RefreshResponse>('/auth/refresh', data);
    return response;
}

export async function logout(refreshToken: string): Promise<void> {
    await api.post('/auth/logout', { refreshToken });
}

export async function verifyFirebaseToken(data: FirebaseVerifyRequest): Promise<FirebaseVerifyResponse> {
    const { data: response } = await api.post<FirebaseVerifyResponse>('/auth/verify-firebase-token', data);
    return response;
}

export async function getProfile(): Promise<UserProfile> {
    const { data } = await api.get<UserProfile>('/users/profile');
    return data;
}

export async function updateProfile(data: UpdateProfileRequest): Promise<UserProfile> {
    const { data: response } = await api.put<UserProfile>('/users/profile', data);
    return response;
}

export async function deleteAccount(data: DeleteAccountRequest): Promise<DeleteAccountResponse> {
    const { data: response } = await api.delete<DeleteAccountResponse>('/users/account', { data });
    return response;
}

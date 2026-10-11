import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit';
import * as authApi from '@/services/authApi';
import { clearAuthTokens, storeAuthTokens } from '@/services/api';
import { getErrorMessage } from '@/utils/apiError';
import type {
    SendOtpRequest,
    SendOtpResponse,
    VerifyOtpRequest,
    VerifyOtpResponse,
    FirebaseVerifyRequest,
    FirebaseVerifyResponse,
    UpdateProfileRequest,
    UserProfile,
    AuthTokensResponse,
} from '@/types/auth';

type RequestStatus = 'idle' | 'loading' | 'succeeded' | 'failed';

export interface AuthState {
    accessToken: string | null;
    refreshToken: string | null;
    accessExpiresAt: string | null;
    user: UserProfile | null;
    isAuthenticated: boolean;
    isNewUser: boolean;
    sendOtpStatus: RequestStatus;
    sendOtpError: string | null;
    verifyOtpStatus: RequestStatus;
    verifyOtpError: string | null;
    firebaseAuthStatus: RequestStatus;
    firebaseAuthError: string | null;
    profileStatus: RequestStatus;
    profileError: string | null;
}

const initialState: AuthState = {
    accessToken: null,
    refreshToken: null,
    accessExpiresAt: null,
    user: null,
    isAuthenticated: false,
    isNewUser: false,
    sendOtpStatus: 'idle',
    sendOtpError: null,
    verifyOtpStatus: 'idle',
    verifyOtpError: null,
    firebaseAuthStatus: 'idle',
    firebaseAuthError: null,
    profileStatus: 'idle',
    profileError: null,
};

export const sendOtp = createAsyncThunk<SendOtpResponse, SendOtpRequest, { rejectValue: string }>(
    'auth/sendOtp',
    async (request, { rejectWithValue }) => {
        try {
            return await authApi.sendOtp(request);
        } catch (error) {
            return rejectWithValue(getErrorMessage(error, 'Failed to send OTP'));
        }
    },
);

export const verifyOtp = createAsyncThunk<VerifyOtpResponse, VerifyOtpRequest, { rejectValue: string }>(
    'auth/verifyOtp',
    async (request, { rejectWithValue }) => {
        try {
            return await authApi.verifyOtp(request);
        } catch (error) {
            return rejectWithValue(getErrorMessage(error, 'Failed to verify OTP'));
        }
    },
);

export const firebaseVerify = createAsyncThunk<
    FirebaseVerifyResponse,
    FirebaseVerifyRequest,
    { rejectValue: string }
>('auth/firebaseVerify', async (request, { rejectWithValue }) => {
    try {
        return await authApi.verifyFirebaseToken(request);
    } catch (error) {
        return rejectWithValue(getErrorMessage(error, 'Failed to verify Firebase token'));
    }
});

export const fetchProfile = createAsyncThunk<UserProfile, void, { rejectValue: string }>(
    'auth/fetchProfile',
    async (_, { rejectWithValue }) => {
        try {
            return await authApi.getProfile();
        } catch (error) {
            return rejectWithValue(getErrorMessage(error, 'Failed to load profile'));
        }
    },
);

export const updateProfile = createAsyncThunk<UserProfile, UpdateProfileRequest, { rejectValue: string }>(
    'auth/updateProfile',
    async (request, { rejectWithValue }) => {
        try {
            return await authApi.updateProfile(request);
        } catch (error) {
            return rejectWithValue(getErrorMessage(error, 'Failed to update profile'));
        }
    },
);

/** Persists the tokens for the axios interceptor after a successful login. */
export const persistSessionTokens = createAsyncThunk<void, void, { state: { auth: AuthState } }>(
    'auth/persistSessionTokens',
    async (_, { getState }) => {
        const { accessToken, refreshToken } = getState().auth;
        if (accessToken && refreshToken) {
            await storeAuthTokens(accessToken, refreshToken);
        }
    },
);

export const logout = createAsyncThunk<void, void, { state: { auth: AuthState } }>(
    'auth/logout',
    async (_, { getState, dispatch }) => {
        const { refreshToken } = getState().auth;
        try {
            if (refreshToken) await authApi.logout(refreshToken);
        } catch {
            // Server-side revocation is best effort; always clear the local session.
        }
        await clearAuthTokens();
        dispatch(clearAuth());
    },
);

export const deleteAccount = createAsyncThunk<void, { otp: string }, { rejectValue: string }>(
    'auth/deleteAccount',
    async ({ otp }, { dispatch, rejectWithValue }) => {
        try {
            await authApi.deleteAccount({ otp });
        } catch (error) {
            return rejectWithValue(getErrorMessage(error, 'Failed to delete account'));
        }
        await clearAuthTokens();
        dispatch(clearAuth());
    },
);

function applyTokens(state: AuthState, payload: AuthTokensResponse) {
    state.accessToken = payload.accessToken;
    state.refreshToken = payload.refreshToken;
    state.accessExpiresAt = payload.accessExpiresAt;
    state.user = payload.user;
    state.isAuthenticated = true;
    state.isNewUser = payload.isNewUser;
}

const authSlice = createSlice({
    name: 'auth',
    initialState,
    reducers: {
        setTokens(
            state,
            action: PayloadAction<{ accessToken: string; refreshToken: string; accessExpiresAt: string }>,
        ) {
            state.accessToken = action.payload.accessToken;
            state.refreshToken = action.payload.refreshToken;
            state.accessExpiresAt = action.payload.accessExpiresAt;
            state.isAuthenticated = true;
        },
        setUser(state, action: PayloadAction<UserProfile>) {
            state.user = action.payload;
        },
        clearAuth() {
            return initialState;
        },
        clearSendOtpError(state) {
            state.sendOtpError = null;
        },
        clearVerifyOtpError(state) {
            state.verifyOtpError = null;
        },
        clearFirebaseAuthError(state) {
            state.firebaseAuthError = null;
        },
        resetSendOtpStatus(state) {
            state.sendOtpStatus = 'idle';
            state.sendOtpError = null;
        },
        clearProfileError(state) {
            state.profileError = null;
        },
        resetProfileStatus(state) {
            state.profileStatus = 'idle';
            state.profileError = null;
        },
    },
    extraReducers: (builder) => {
        builder
            .addCase(sendOtp.pending, (state) => {
                state.sendOtpStatus = 'loading';
                state.sendOtpError = null;
            })
            .addCase(sendOtp.fulfilled, (state) => {
                state.sendOtpStatus = 'succeeded';
            })
            .addCase(sendOtp.rejected, (state, action) => {
                state.sendOtpStatus = 'failed';
                state.sendOtpError = action.payload ?? 'Failed to send OTP';
            })
            .addCase(verifyOtp.pending, (state) => {
                state.verifyOtpStatus = 'loading';
                state.verifyOtpError = null;
            })
            .addCase(verifyOtp.fulfilled, (state, action) => {
                state.verifyOtpStatus = 'succeeded';
                applyTokens(state, action.payload);
            })
            .addCase(verifyOtp.rejected, (state, action) => {
                state.verifyOtpStatus = 'failed';
                state.verifyOtpError = action.payload ?? 'Failed to verify OTP';
            })
            .addCase(firebaseVerify.pending, (state) => {
                state.firebaseAuthStatus = 'loading';
                state.firebaseAuthError = null;
            })
            .addCase(firebaseVerify.fulfilled, (state, action) => {
                state.firebaseAuthStatus = 'succeeded';
                applyTokens(state, action.payload);
            })
            .addCase(firebaseVerify.rejected, (state, action) => {
                state.firebaseAuthStatus = 'failed';
                state.firebaseAuthError = action.payload ?? 'Failed to verify Firebase token';
            })
            .addCase(fetchProfile.fulfilled, (state, action) => {
                state.user = action.payload;
            })
            .addCase(updateProfile.pending, (state) => {
                state.profileStatus = 'loading';
                state.profileError = null;
            })
            .addCase(updateProfile.fulfilled, (state, action) => {
                state.profileStatus = 'succeeded';
                state.user = action.payload;
            })
            .addCase(updateProfile.rejected, (state, action) => {
                state.profileStatus = 'failed';
                state.profileError = action.payload ?? 'Failed to update profile';
            });
    },
});

export const {
    setTokens,
    setUser,
    clearAuth,
    clearSendOtpError,
    clearVerifyOtpError,
    clearFirebaseAuthError,
    resetSendOtpStatus,
    clearProfileError,
    resetProfileStatus,
} = authSlice.actions;
export default authSlice.reducer;

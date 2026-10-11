import axios from 'axios';
import Constants from 'expo-constants';
import { secureStorage } from './secureStorage';

export const API_URL: string = Constants.expoConfig?.extra?.apiUrl ?? 'http://127.0.0.1:8080/api/v1';

export const JWE_KEY = 'jweToken';
export const ACCESS_TOKEN_KEY = 'accessToken';
export const REFRESH_TOKEN_KEY = 'refreshToken';

const api = axios.create({
    baseURL: API_URL,
    timeout: 30_000,
    headers: { 'Content-Type': 'application/json', 'X-Platform': 'MOBILE' },
});

let isRefreshing = false;
let failedQueue: {
    resolve: (token: string) => void;
    reject: (error: unknown) => void;
}[] = [];

function processQueue(error: unknown, token: string | null = null): void {
    failedQueue.forEach((promise) => {
        if (error) {
            promise.reject(error);
        } else if (token) {
            promise.resolve(token);
        }
    });
    failedQueue = [];
}

/** Returns the JWT if logged in, otherwise the anonymous JWE (or null). */
export async function getBearerToken(): Promise<string | null> {
    const accessToken = await secureStorage.getItem(ACCESS_TOKEN_KEY);
    if (accessToken) return accessToken;
    return secureStorage.getItem(JWE_KEY);
}

export async function storeAuthTokens(accessToken: string, refreshToken: string): Promise<void> {
    await secureStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    await secureStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export async function clearAuthTokens(): Promise<void> {
    await secureStorage.removeItem(ACCESS_TOKEN_KEY);
    await secureStorage.removeItem(REFRESH_TOKEN_KEY);
}

api.interceptors.request.use(async (config) => {
    const token = await getBearerToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

const NO_REFRESH_PATHS = ['/auth/refresh', '/auth/send-otp', '/auth/verify-otp', '/auth/verify-firebase-token'];

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
            if (NO_REFRESH_PATHS.some((path) => originalRequest.url?.includes(path))) {
                return Promise.reject(error);
            }

            const storedRefreshToken = await secureStorage.getItem(REFRESH_TOKEN_KEY);
            if (!storedRefreshToken) {
                // Anonymous session: nothing to refresh.
                return Promise.reject(error);
            }

            if (isRefreshing) {
                return new Promise<string>((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                }).then((token) => {
                    originalRequest.headers.Authorization = `Bearer ${token}`;
                    return api(originalRequest);
                });
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                const { data } = await axios.post(
                    `${API_URL}/auth/refresh`,
                    { refreshToken: storedRefreshToken },
                    { headers: { 'Content-Type': 'application/json', 'X-Platform': 'MOBILE' } },
                );

                await storeAuthTokens(data.accessToken, data.refreshToken);

                originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
                processQueue(null, data.accessToken);

                return api(originalRequest);
            } catch (refreshError) {
                processQueue(refreshError, null);
                await clearAuthTokens();
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }

        return Promise.reject(error);
    },
);

export default api;

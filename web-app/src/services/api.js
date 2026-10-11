import axios from "axios";
import { isTokenValid } from "../utils/tokenUtils";
import { clearToken } from "../redux/tokenSlice";

export const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api/v1";

// Injected by redux/store.js to avoid a circular import (store -> slices -> api -> store).
let store = null;
export const injectStore = (s) => {
    store = s;
};

export const getValidToken = () => {
    if (!store) return null;
    const { jweToken, expiresAt } = store.getState().token;
    return isTokenValid(jweToken, expiresAt) ? jweToken : null;
};

const api = axios.create({
    baseURL: API_URL,
    headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
    const token = getValidToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    config.headers["X-Platform"] = "WEB";
    return config;
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401 && store) {
            store.dispatch(clearToken());
        }
        return Promise.reject(error);
    }
);

/** Extracts a human-readable message from an RFC 7807 problem response or axios error. */
export const getErrorMessage = (err) => {
    const data = err?.response?.data;
    if (data && typeof data === "object") {
        return data.detail || data.title || data.message || err.message;
    }
    return err?.message || "Unknown error";
};

export const verifyTurnstile = (token) => api.post("/turnstile/verify", { token });

export const getFeatures = () => api.get("/features");

export const submitEcho = ({ message, delaySec }) => api.post("/jobs/echo", { message, delaySec });

export const getJobStatus = (jobId) => api.get(`/jobs/${encodeURIComponent(jobId)}/status`);

export const getJobStreamUrl = (jobId) => `${API_URL}/jobs/${encodeURIComponent(jobId)}/stream`;

export const getJobStreamHeaders = () => {
    const headers = { "X-Platform": "WEB", Accept: "text/event-stream" };
    const token = getValidToken();
    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }
    return headers;
};

export default api;

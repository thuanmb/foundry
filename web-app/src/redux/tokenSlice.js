import { createSlice } from "@reduxjs/toolkit";

const TOKEN_KEY = "jwe_token";
const EXPIRES_KEY = "jwe_expires_at";

const tokenSlice = createSlice({
    name: "token",
    initialState: {
        jweToken: null,
        expiresAt: null,
    },
    reducers: {
        setAnonymousToken: (state, action) => {
            state.jweToken = action.payload.jweToken;
            state.expiresAt = action.payload.expiresAt;
            sessionStorage.setItem(TOKEN_KEY, action.payload.jweToken);
            sessionStorage.setItem(EXPIRES_KEY, action.payload.expiresAt);
        },
        clearToken: (state) => {
            state.jweToken = null;
            state.expiresAt = null;
            sessionStorage.removeItem(TOKEN_KEY);
            sessionStorage.removeItem(EXPIRES_KEY);
        },
        hydrateToken: (state) => {
            const storedToken = sessionStorage.getItem(TOKEN_KEY);
            const storedExpiresAt = sessionStorage.getItem(EXPIRES_KEY);
            if (storedToken && storedExpiresAt) {
                state.jweToken = storedToken;
                state.expiresAt = storedExpiresAt;
            }
        },
    },
});

export const { setAnonymousToken, clearToken, hydrateToken } = tokenSlice.actions;
export default tokenSlice.reducer;

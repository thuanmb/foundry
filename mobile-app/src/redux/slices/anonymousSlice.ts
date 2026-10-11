import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

interface AnonymousState {
    jweToken: string | null;
    jweExpiresAt: string | null;
}

const initialState: AnonymousState = {
    jweToken: null,
    jweExpiresAt: null,
};

const anonymousSlice = createSlice({
    name: 'anonymous',
    initialState,
    reducers: {
        setJweToken(
            state,
            action: PayloadAction<{ token: string; expiresAt: string }>,
        ) {
            state.jweToken = action.payload.token;
            state.jweExpiresAt = action.payload.expiresAt;
        },
        clearJweToken(state) {
            state.jweToken = null;
            state.jweExpiresAt = null;
        },
    },
});

export const { setJweToken, clearJweToken } = anonymousSlice.actions;
export default anonymousSlice.reducer;

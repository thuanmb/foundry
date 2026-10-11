import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { fetchMobileConfig } from '@/services/configApi';
import { getErrorMessage } from '@/utils/apiError';
import type { MobileConfigPollingConfig, MobileConfigResponse } from '@/types/config';

export interface FeatureFlags {
    notes: boolean;
    echo: boolean;
}

export interface PollingConfig {
    initialIntervalMs: number;
    maxIntervalMs: number;
    backoffMultiplier: number;
    maxAttempts: number;
}

export type ConfigStatus = 'idle' | 'loading' | 'succeeded' | 'failed';

export interface ConfigState {
    status: ConfigStatus;
    error: string | null;
    features: FeatureFlags;
    /** Raw flag map from the server, for flags without a typed field yet. */
    rawFeatures: Record<string, boolean>;
    pollingConfig: PollingConfig;
    minAppVersion: string;
    lastFetched: string | null;
}

export const initialConfigState: ConfigState = {
    status: 'idle',
    error: null,
    features: {
        notes: true,
        echo: true,
    },
    rawFeatures: {},
    pollingConfig: {
        initialIntervalMs: 2000,
        maxIntervalMs: 15000,
        backoffMultiplier: 2,
        maxAttempts: 40,
    },
    minAppVersion: '1.0.0',
    lastFetched: null,
};

function mapFeatures(features: Record<string, boolean> | undefined): FeatureFlags {
    return {
        notes: features?.notes ?? initialConfigState.features.notes,
        echo: features?.echo ?? initialConfigState.features.echo,
    };
}

function mapPollingConfig(config: MobileConfigPollingConfig | undefined): PollingConfig {
    if (!config) return initialConfigState.pollingConfig;
    return {
        ...initialConfigState.pollingConfig,
        initialIntervalMs: config.initialDelaySec * 1000,
        maxIntervalMs: config.maxDelaySec * 1000,
        backoffMultiplier: config.backoffMultiplier,
    };
}

export const fetchConfig = createAsyncThunk<MobileConfigResponse, void, { rejectValue: string }>(
    'config/fetch',
    async (_, { rejectWithValue }) => {
        try {
            return await fetchMobileConfig();
        } catch (error) {
            return rejectWithValue(getErrorMessage(error, 'Failed to load app config'));
        }
    },
);

const configSlice = createSlice({
    name: 'config',
    initialState: initialConfigState,
    reducers: {},
    extraReducers: (builder) => {
        builder
            .addCase(fetchConfig.pending, (state) => {
                state.status = 'loading';
                state.error = null;
            })
            .addCase(fetchConfig.fulfilled, (state, action) => {
                const dto = action.payload;
                state.status = 'succeeded';
                state.features = mapFeatures(dto.features);
                state.rawFeatures = dto.features ?? {};
                state.pollingConfig = mapPollingConfig(dto.pollingConfig ?? dto.polling);
                state.minAppVersion = dto.minAppVersion ?? state.minAppVersion;
                state.lastFetched = new Date().toISOString();
            })
            .addCase(fetchConfig.rejected, (state, action) => {
                state.status = 'failed';
                state.error = action.payload ?? 'Failed to load app config';
            });
    },
});

export default configSlice.reducer;

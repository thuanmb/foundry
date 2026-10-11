import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit';
import * as jobsApi from '@/services/jobsApi';
import { getErrorMessage } from '@/utils/apiError';
import type {
    EchoJobRequest,
    EchoResult,
    JobError,
    JobStatus,
    JobStatusResponse,
    JobSubmitResponse,
} from '@/types/job';

export interface JobEntry {
    jobId: string;
    status: JobStatus;
    message?: string;
    estimatedWaitSec?: number | null;
    result?: EchoResult | null;
    error?: JobError | null;
    updatedAt: string;
}

export interface JobsState {
    byId: Record<string, JobEntry>;
    submitStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
    submitError: string | null;
}

const initialState: JobsState = {
    byId: {},
    submitStatus: 'idle',
    submitError: null,
};

export const isTerminalStatus = (status: JobStatus | undefined) =>
    status === 'COMPLETED' || status === 'FAILED';

export const submitEchoJob = createAsyncThunk<JobSubmitResponse, EchoJobRequest, { rejectValue: string }>(
    'jobs/submitEcho',
    async (request, { rejectWithValue }) => {
        try {
            return await jobsApi.submitEchoJob(request);
        } catch (error) {
            return rejectWithValue(getErrorMessage(error, 'Failed to submit job'));
        }
    },
);

export const pollJobStatus = createAsyncThunk<JobStatusResponse, string, { rejectValue: string }>(
    'jobs/pollStatus',
    async (jobId, { rejectWithValue }) => {
        try {
            return await jobsApi.fetchJobStatus(jobId);
        } catch (error) {
            return rejectWithValue(getErrorMessage(error, 'Failed to fetch job status'));
        }
    },
);

function upsert(state: JobsState, jobId: string, patch: Partial<JobEntry>) {
    const existing = state.byId[jobId];
    // Never regress a job that already reached a terminal state.
    if (existing && isTerminalStatus(existing.status) && patch.status && !isTerminalStatus(patch.status)) {
        return;
    }
    state.byId[jobId] = {
        ...(existing ?? { jobId, status: 'PENDING' }),
        ...patch,
        jobId,
        updatedAt: new Date().toISOString(),
    };
}

const jobsSlice = createSlice({
    name: 'jobs',
    initialState,
    reducers: {
        jobStatusUpdated(state, action: PayloadAction<Partial<JobStatusResponse> & { jobId: string }>) {
            const { jobId, status, estimatedWaitSec } = action.payload;
            upsert(state, jobId, {
                ...(status ? { status } : {}),
                ...(estimatedWaitSec !== undefined ? { estimatedWaitSec } : {}),
            });
        },
        jobCompleted(state, action: PayloadAction<{ jobId: string; result: EchoResult }>) {
            upsert(state, action.payload.jobId, {
                status: 'COMPLETED',
                result: action.payload.result,
                error: null,
            });
        },
        jobFailed(state, action: PayloadAction<{ jobId: string; error: JobError }>) {
            upsert(state, action.payload.jobId, { status: 'FAILED', error: action.payload.error });
        },
        clearSubmitError(state) {
            state.submitStatus = 'idle';
            state.submitError = null;
        },
        clearJobs() {
            return initialState;
        },
    },
    extraReducers: (builder) => {
        builder
            .addCase(submitEchoJob.pending, (state) => {
                state.submitStatus = 'loading';
                state.submitError = null;
            })
            .addCase(submitEchoJob.fulfilled, (state, action) => {
                state.submitStatus = 'succeeded';
                upsert(state, action.payload.jobId, {
                    status: action.payload.status,
                    message: action.meta.arg.message,
                });
            })
            .addCase(submitEchoJob.rejected, (state, action) => {
                state.submitStatus = 'failed';
                state.submitError = action.payload ?? 'Failed to submit job';
            })
            .addCase(pollJobStatus.fulfilled, (state, action) => {
                const { jobId, status, estimatedWaitSec, result, error } = action.payload;
                upsert(state, jobId, {
                    status,
                    estimatedWaitSec,
                    ...(result !== undefined ? { result } : {}),
                    ...(error !== undefined ? { error } : {}),
                });
            });
    },
});

export const { jobStatusUpdated, jobCompleted, jobFailed, clearSubmitError, clearJobs } = jobsSlice.actions;
export default jobsSlice.reducer;

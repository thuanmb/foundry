import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import { submitEcho, getJobStatus, getErrorMessage } from "../services/api";

export const TERMINAL_STATUSES = ["COMPLETED", "FAILED"];

export const submitEchoJob = createAsyncThunk(
    "jobs/submitEcho",
    async ({ message, delaySec }, { rejectWithValue }) => {
        try {
            const response = await submitEcho({ message, delaySec });
            return response.data;
        } catch (err) {
            return rejectWithValue(getErrorMessage(err));
        }
    }
);

export const fetchJobStatus = createAsyncThunk(
    "jobs/fetchStatus",
    async (jobId, { rejectWithValue }) => {
        try {
            const response = await getJobStatus(jobId);
            return response.data;
        } catch (err) {
            return rejectWithValue(getErrorMessage(err));
        }
    }
);

function mergeJob(state, update) {
    if (!update?.jobId) return;
    const existing = state.byId[update.jobId] || { jobId: update.jobId, history: [] };
    const job = { ...existing, ...update, history: existing.history };
    if (update.status && existing.status !== update.status) {
        job.history = [...existing.history, { status: update.status, at: new Date().toISOString() }];
    }
    state.byId[update.jobId] = job;
}

const jobsSlice = createSlice({
    name: "jobs",
    initialState: {
        byId: {},
        submitting: false,
        submitError: null,
        fetchError: null,
    },
    reducers: {
        jobUpdated: (state, action) => {
            mergeJob(state, action.payload);
        },
    },
    extraReducers: (builder) => {
        builder
            .addCase(submitEchoJob.pending, (state) => {
                state.submitting = true;
                state.submitError = null;
            })
            .addCase(submitEchoJob.fulfilled, (state, action) => {
                state.submitting = false;
                mergeJob(state, action.payload);
            })
            .addCase(submitEchoJob.rejected, (state, action) => {
                state.submitting = false;
                state.submitError = action.payload ?? action.error.message;
            })
            .addCase(fetchJobStatus.pending, (state) => {
                state.fetchError = null;
            })
            .addCase(fetchJobStatus.fulfilled, (state, action) => {
                mergeJob(state, action.payload);
            })
            .addCase(fetchJobStatus.rejected, (state, action) => {
                state.fetchError = action.payload ?? action.error.message;
            });
    },
});

export const { jobUpdated } = jobsSlice.actions;
export const selectJob = (jobId) => (state) => state.jobs.byId[jobId];
export default jobsSlice.reducer;

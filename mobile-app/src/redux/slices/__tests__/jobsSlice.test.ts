import { configureStore } from '@reduxjs/toolkit';
import jobsReducer, {
    isTerminalStatus,
    jobCompleted,
    jobFailed,
    jobStatusUpdated,
    pollJobStatus,
    submitEchoJob,
} from '../jobsSlice';
import * as jobsApi from '@/services/jobsApi';

jest.mock('@/services/jobsApi');

const mockedApi = jobsApi as jest.Mocked<typeof jobsApi>;
const result = { echo: 'hi', length: 2, processedAt: '2026-01-01T00:00:00Z' };

function makeStore() {
    return configureStore({ reducer: { jobs: jobsReducer } });
}

describe('jobsSlice', () => {
    beforeEach(() => jest.resetAllMocks());

    it('identifies terminal statuses', () => {
        expect(isTerminalStatus('COMPLETED')).toBe(true);
        expect(isTerminalStatus('FAILED')).toBe(true);
        expect(isTerminalStatus('RUNNING')).toBe(false);
        expect(isTerminalStatus(undefined)).toBe(false);
    });

    it('stores a submitted echo job', async () => {
        const store = makeStore();
        mockedApi.submitEchoJob.mockResolvedValueOnce({ jobId: 'j1', status: 'PENDING' });
        await store.dispatch(submitEchoJob({ message: 'hi', delaySec: 3 }));
        const state = store.getState().jobs;
        expect(state.submitStatus).toBe('succeeded');
        expect(state.byId.j1).toMatchObject({ jobId: 'j1', status: 'PENDING', message: 'hi' });
    });

    it('records submit errors', async () => {
        const store = makeStore();
        mockedApi.submitEchoJob.mockRejectedValueOnce({ response: { data: { title: 'Feature disabled' } } });
        await store.dispatch(submitEchoJob({ message: 'hi', delaySec: 0 }));
        expect(store.getState().jobs.submitStatus).toBe('failed');
        expect(store.getState().jobs.submitError).toBe('Feature disabled');
    });

    it('applies SSE status, completed and failed events', () => {
        let state = jobsReducer(undefined, jobStatusUpdated({ jobId: 'j1', status: 'RUNNING', estimatedWaitSec: 5 }));
        expect(state.byId.j1).toMatchObject({ status: 'RUNNING', estimatedWaitSec: 5 });

        state = jobsReducer(state, jobCompleted({ jobId: 'j1', result }));
        expect(state.byId.j1).toMatchObject({ status: 'COMPLETED', result });

        state = jobsReducer(state, jobFailed({ jobId: 'j2', error: { code: 'JOB_FAILED', message: 'x' } }));
        expect(state.byId.j2).toMatchObject({ status: 'FAILED', error: { code: 'JOB_FAILED' } });
    });

    it('never regresses a terminal job', () => {
        let state = jobsReducer(undefined, jobCompleted({ jobId: 'j1', result }));
        state = jobsReducer(state, jobStatusUpdated({ jobId: 'j1', status: 'RUNNING' }));
        expect(state.byId.j1.status).toBe('COMPLETED');
    });

    it('merges polled status including result', async () => {
        const store = makeStore();
        mockedApi.fetchJobStatus.mockResolvedValueOnce({ jobId: 'j1', status: 'COMPLETED', result });
        await store.dispatch(pollJobStatus('j1'));
        expect(store.getState().jobs.byId.j1).toMatchObject({ status: 'COMPLETED', result });
    });
});

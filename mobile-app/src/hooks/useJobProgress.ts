import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { selectPollingConfig } from '@/redux/selectors';
import { jobCompleted, jobFailed, jobStatusUpdated, pollJobStatus, isTerminalStatus } from '@/redux/slices/jobsSlice';
import { openJobStream } from '@/services/jobsApi';

/**
 * Tracks a job until it reaches a terminal state: SSE first, then falls back to
 * polling with exponential backoff (config.pollingConfig) if the stream fails.
 */
export function useJobProgress(jobId: string | undefined) {
    const dispatch = useAppDispatch();
    const polling = useAppSelector(selectPollingConfig);

    useEffect(() => {
        if (!jobId) return;

        let cancelled = false;
        let timer: ReturnType<typeof setTimeout> | null = null;
        let attempts = 0;
        let delay = polling.initialIntervalMs;

        const poll = async () => {
            if (cancelled) return;
            attempts += 1;
            const action = await dispatch(pollJobStatus(jobId));
            if (cancelled) return;
            const status = pollJobStatus.fulfilled.match(action) ? action.payload.status : undefined;
            if (isTerminalStatus(status) || attempts >= polling.maxAttempts) return;
            timer = setTimeout(poll, delay);
            delay = Math.min(delay * polling.backoffMultiplier, polling.maxIntervalMs);
        };

        // Snapshot first, in case the job finished before the stream connects.
        void dispatch(pollJobStatus(jobId));

        const closeStream = openJobStream(jobId, {
            onStatus: (data) => dispatch(jobStatusUpdated({ ...data, jobId })),
            onCompleted: (result) => dispatch(jobCompleted({ jobId, result })),
            onFailed: (error) => dispatch(jobFailed({ jobId, error })),
            onError: () => {
                if (!cancelled) void poll();
            },
        });

        return () => {
            cancelled = true;
            closeStream();
            if (timer) clearTimeout(timer);
        };
    }, [jobId, dispatch, polling]);
}

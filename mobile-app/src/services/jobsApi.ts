import EventSource from 'react-native-sse';
import api, { API_URL, getBearerToken } from './api';
import type {
    EchoJobRequest,
    EchoResult,
    JobError,
    JobStatusResponse,
    JobSubmitResponse,
} from '@/types/job';

export async function submitEchoJob(request: EchoJobRequest): Promise<JobSubmitResponse> {
    const { data } = await api.post<JobSubmitResponse>('/jobs/echo', request);
    return data;
}

export async function fetchJobStatus(jobId: string): Promise<JobStatusResponse> {
    const { data } = await api.get<JobStatusResponse>(`/jobs/${encodeURIComponent(jobId)}/status`);
    return data;
}

export interface JobStreamHandlers {
    onStatus: (data: Partial<JobStatusResponse>) => void;
    onCompleted: (result: EchoResult) => void;
    onFailed: (error: JobError) => void;
    /** Connection-level failure; callers should fall back to polling. */
    onError: (error: Error) => void;
}

type JobEvent = 'status' | 'completed' | 'failed';

const UNKNOWN_ERROR: JobError = { code: 'UNKNOWN', message: 'Job failed' };

function parse<T>(raw: string | null | undefined): T | null {
    try {
        return JSON.parse(raw ?? '') as T;
    } catch {
        return null;
    }
}

/** Opens an SSE stream for a job. Returns a function that closes the stream. */
export function openJobStream(jobId: string, handlers: JobStreamHandlers): () => void {
    const url = `${API_URL}/jobs/${encodeURIComponent(jobId)}/stream`;
    let closed = false;
    let es: EventSource<JobEvent> | null = null;

    const close = () => {
        if (closed) return;
        closed = true;
        es?.removeAllEventListeners();
        es?.close();
        es = null;
    };

    void (async () => {
        const token = await getBearerToken();
        if (closed) return;

        const headers: Record<string, string> = {
            Accept: 'text/event-stream',
            'X-Platform': 'MOBILE',
        };
        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }

        es = new EventSource<JobEvent>(url, { headers, pollingInterval: 0 });

        es.addEventListener('status', (event) => {
            const data = parse<Partial<JobStatusResponse>>(event.data);
            if (data) handlers.onStatus(data);
        });

        es.addEventListener('completed', (event) => {
            const data = parse<{ jobId: string; result: EchoResult }>(event.data);
            close();
            if (data?.result) {
                handlers.onCompleted(data.result);
            } else {
                handlers.onError(new Error('Malformed completed event'));
            }
        });

        es.addEventListener('failed', (event) => {
            const data = parse<{ jobId: string; error: JobError }>(event.data);
            close();
            handlers.onFailed(data?.error ?? UNKNOWN_ERROR);
        });

        es.addEventListener('error', () => {
            if (closed) return;
            close();
            handlers.onError(new Error('SSE connection failed'));
        });
    })();

    return close;
}

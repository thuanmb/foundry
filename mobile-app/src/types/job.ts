export type JobStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface EchoJobRequest {
    message: string;
    delaySec: number;
}

export interface JobSubmitResponse {
    jobId: string;
    status: JobStatus;
}

export interface EchoResult {
    echo: string;
    length: number;
    processedAt: string;
    llmReply?: string | null;
}

export interface JobError {
    code: string;
    message: string;
}

export interface JobStatusResponse {
    jobId: string;
    status: JobStatus;
    estimatedWaitSec?: number | null;
    result?: EchoResult | null;
    error?: JobError | null;
}

export const ECHO_MESSAGE_MAX = 500;
export const ECHO_DELAY_MAX = 30;
export const ECHO_DELAY_OPTIONS = [0, 3, 5, 10] as const;

import type { ProblemDetail } from '@/types/auth';

/** Extracts a human-readable message from an axios error carrying an RFC 7807 body. */
export function getErrorMessage(error: unknown, fallback: string): string {
    const err = error as { response?: { data?: ProblemDetail }; message?: string } | null;
    const body = err?.response?.data;
    if (body && typeof body === 'object') {
        if (typeof body.detail === 'string' && body.detail) return body.detail;
        if (typeof body.title === 'string' && body.title) return body.title;
    }
    return err?.message || fallback;
}

export function getErrorStatus(error: unknown): number | undefined {
    return (error as { response?: { status?: number } } | null)?.response?.status;
}

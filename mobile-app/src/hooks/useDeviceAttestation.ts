import { useState, useEffect, useCallback, useRef } from 'react';
import {
    performAttestation,
    getStoredJweToken,
    storeJweToken,
} from '@/services/attestation';
import { useAppDispatch } from '@/redux/hooks';
import { setJweToken, clearJweToken } from '@/redux/slices/anonymousSlice';

type AttestationState =
    | { status: 'idle' }
    | { status: 'checking' }
    | { status: 'verifying' }
    | { status: 'success'; token: string }
    | { status: 'failed'; reason: string };

export function useDeviceAttestation() {
    const [state, setState] = useState<AttestationState>({ status: 'checking' });
    const dispatch = useAppDispatch();
    const hasRun = useRef(false);

    const runAttestation = useCallback(async () => {
        if (hasRun.current) return;
        hasRun.current = true;

        // 1. Check if we already have a valid JWE token
        const existingToken = await getStoredJweToken();
        if (existingToken) {
            dispatch(setJweToken({ token: existingToken, expiresAt: '' }));
            setState({ status: 'success', token: existingToken });
            return;
        }

        // 2. Perform attestation (with dev bypass fallback for simulators)
        setState({ status: 'verifying' });

        const result = await performAttestation();
        if (result) {
            await storeJweToken(result.token, result.expiresAt);
            dispatch(
                setJweToken({ token: result.token, expiresAt: result.expiresAt }),
            );
            setState({ status: 'success', token: result.token });
        } else {
            dispatch(clearJweToken());
            setState({
                status: 'failed',
                reason: 'verification_failed',
            });
        }
    }, [dispatch]);

    useEffect(() => {
        runAttestation();
    }, [runAttestation]);

    const retry = useCallback(() => {
        hasRun.current = false;
        setState({ status: 'checking' });
        runAttestation();
    }, [runAttestation]);

    return { ...state, retry };
}

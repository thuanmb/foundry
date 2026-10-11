import Constants from 'expo-constants';
import { getAuth, signInWithPhoneNumber } from '@react-native-firebase/auth';
import type { ConfirmationResult } from '@react-native-firebase/auth';

export { getAuth, signInWithPhoneNumber };
export type { ConfirmationResult };

export type AuthProvider = 'firebase' | 'backend';

/** Set via app.config.ts `extra.authProvider` (defaults to 'firebase' when Firebase config files exist). */
export const AUTH_PROVIDER: AuthProvider =
    Constants.expoConfig?.extra?.authProvider === 'firebase' ? 'firebase' : 'backend';

let confirmationResult: ConfirmationResult | null = null;

export function setConfirmationResult(result: ConfirmationResult | null) {
    confirmationResult = result;
}

export function getConfirmationResult(): ConfirmationResult | null {
    return confirmationResult;
}

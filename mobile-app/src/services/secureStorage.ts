import * as SecureStore from 'expo-secure-store';

/**
 * Sanitize a Redux Persist key for SecureStore compatibility.
 * SecureStore only allows alphanumeric characters, '.', '-', and '_'.
 * Redux Persist uses ':' in its composite keys (e.g., "persist:auth").
 */
function sanitizeKey(key: string): string {
    return key.replace(/[^a-zA-Z0-9._-]/g, '_');
}

export const secureStorage = {
    getItem: async (key: string): Promise<string | null> => {
        return SecureStore.getItemAsync(sanitizeKey(key));
    },
    setItem: async (key: string, value: string): Promise<void> => {
        await SecureStore.setItemAsync(sanitizeKey(key), value);
    },
    removeItem: async (key: string): Promise<void> => {
        await SecureStore.deleteItemAsync(sanitizeKey(key));
    },
};

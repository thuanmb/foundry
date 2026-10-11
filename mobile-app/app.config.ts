import fs from 'node:fs';
import path from 'node:path';
import type { ExpoConfig, ConfigContext } from 'expo/config';

type AppEnv = 'development' | 'staging' | 'production';
const APP_ENV = (process.env.APP_ENV ?? 'development') as AppEnv;

const variants = {
    development: {
        name: 'Foundry (Dev)',
        idSuffix: '.dev',
        apiUrl: process.env.EXPO_PUBLIC_API_URL ?? 'http://127.0.0.1:8080/api/v1',
    },
    staging: {
        name: 'Foundry (Staging)',
        idSuffix: '.staging',
        apiUrl: process.env.EXPO_PUBLIC_API_URL ?? 'https://api-staging.example.com/api/v1',
    },
    production: {
        name: 'Foundry',
        idSuffix: '',
        apiUrl: process.env.EXPO_PUBLIC_API_URL ?? 'https://api.example.com/api/v1',
    },
} as const;

const v = variants[APP_ENV];
const BUNDLE_ID = `com.thuanbui.foundry${v.idSuffix}`;
const BACKGROUND_COLOR = '#111827';

// Firebase config files are git-ignored; only reference them when present so `expo start` works without Firebase.
function optionalFile(relativePath: string): string | undefined {
    return fs.existsSync(path.join(__dirname, relativePath)) ? relativePath : undefined;
}

const iosGoogleServicesFile = optionalFile('./GoogleService-Info.plist');
const androidGoogleServicesFile = optionalFile('./google-services.json');
const hasFirebase = Boolean(iosGoogleServicesFile || androidGoogleServicesFile);
// 'firebase' = Firebase phone auth; 'backend' = server-side SMS OTP (/auth/send-otp + /auth/verify-otp).
const authProvider = process.env.EXPO_PUBLIC_AUTH_PROVIDER ?? (hasFirebase ? 'firebase' : 'backend');

export default ({ config }: ConfigContext): ExpoConfig => ({
    ...config,
    name: v.name,
    slug: 'foundry',
    scheme: 'foundry',
    version: '1.0.0',
    orientation: 'portrait',
    userInterfaceStyle: 'automatic',
    icon: './assets/images/icon.png',
    runtimeVersion: { policy: 'appVersion' },
    ios: {
        bundleIdentifier: BUNDLE_ID,
        supportsTablet: false,
        ...(iosGoogleServicesFile ? { googleServicesFile: iosGoogleServicesFile } : {}),
        infoPlist: {
            CFBundleAllowMixedLocalizations: true,
        },
    },
    android: {
        package: BUNDLE_ID,
        ...(androidGoogleServicesFile ? { googleServicesFile: androidGoogleServicesFile } : {}),
        adaptiveIcon: {
            foregroundImage: './assets/images/android-icon-foreground.png',
            backgroundColor: BACKGROUND_COLOR,
        },
        permissions: ['INTERNET'],
    },
    plugins: [
        'expo-router',
        'expo-secure-store',
        'expo-localization',
        'expo-sharing',
        [
            'expo-build-properties',
            {
                ios: {
                    useFrameworks: 'static',
                },
            },
        ],
        ...(hasFirebase ? ['@react-native-firebase/app', '@react-native-firebase/auth'] : []),
        [
            'expo-splash-screen',
            {
                image: './assets/images/splash.png',
                resizeMode: 'contain',
                backgroundColor: BACKGROUND_COLOR,
            },
        ],
    ],
    experiments: {
        typedRoutes: true,
        reactCompiler: true,
    },
    extra: {
        appEnv: APP_ENV,
        apiUrl: v.apiUrl,
        authProvider,
        eas: { projectId: '<eas-project-id>' },
        playIntegrityProjectNumber: process.env.EXPO_PUBLIC_PLAY_INTEGRITY_PROJECT_NUMBER ?? '',
    },
});

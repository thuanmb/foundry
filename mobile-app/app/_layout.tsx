import 'react-native-reanimated';
import { useEffect } from 'react';
import { StatusBar } from 'expo-status-bar';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { Provider } from 'react-redux';
import { PersistGate } from 'redux-persist/integration/react';
import { store, persistor } from '@/redux/store';
import { useAppDispatch } from '@/redux/hooks';
import { useDeviceAttestation } from '@/hooks/useDeviceAttestation';
import { fetchConfig } from '@/redux/slices/configSlice';
import { loadSavedLanguage } from '@/i18n';
import { colors } from '@/theme';

SplashScreen.preventAutoHideAsync();

function AppContent() {
    const dispatch = useAppDispatch();

    useDeviceAttestation();

    useEffect(() => {
        loadSavedLanguage().finally(() => SplashScreen.hideAsync());
    }, []);

    // Remote config: feature flags + polling settings
    useEffect(() => {
        dispatch(fetchConfig());
    }, [dispatch]);

    return (
        <>
            <StatusBar style="light" />
            <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.background } }}>
                <Stack.Screen name="(tabs)" />
                <Stack.Screen
                    name="(auth)"
                    options={{ presentation: 'modal', animation: 'slide_from_bottom' }}
                />
            </Stack>
        </>
    );
}

export default function RootLayout() {
    return (
        <Provider store={store}>
            <PersistGate loading={null} persistor={persistor}>
                <AppContent />
            </PersistGate>
        </Provider>
    );
}

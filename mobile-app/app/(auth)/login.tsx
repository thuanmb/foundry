import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View, Text, StyleSheet, Alert, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import Ionicons from '@expo/vector-icons/Ionicons';
import { colors, typography, spacing } from '@/theme';
import Card from '@/components/Card';
import { PhoneInput } from '@/components/account';
import { useAppDispatch } from '@/redux/hooks';
import { sendOtp } from '@/redux/slices';
import { AUTH_PROVIDER, getAuth, signInWithPhoneNumber, setConfirmationResult } from '@/services/firebase';

export default function LoginScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const dispatch = useAppDispatch();
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = useCallback(
        async (phoneNumber: string) => {
            setSubmitting(true);
            try {
                if (AUTH_PROVIDER === 'firebase') {
                    const confirmation = await signInWithPhoneNumber(getAuth(), phoneNumber);
                    setConfirmationResult(confirmation);
                } else {
                    await dispatch(sendOtp({ phoneNumber })).unwrap();
                }
                router.push({ pathname: '/(auth)/otp-verify', params: { phone: phoneNumber } });
            } catch (error: unknown) {
                const code = (error as { code?: string } | null)?.code;
                const message =
                    code === 'auth/too-many-requests'
                        ? t('auth.rateLimited')
                        : code === 'auth/invalid-phone-number'
                            ? t('auth.invalidPhone')
                            : t('auth.sendOtpError');
                Alert.alert('', message);
            } finally {
                setSubmitting(false);
            }
        },
        [dispatch, router, t],
    );

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <TouchableOpacity
                    onPress={() => router.back()}
                    accessibilityRole="button"
                    accessibilityLabel={t('common.close')}
                    hitSlop={12}
                >
                    <Ionicons name="close" size={24} color={colors.text} />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>{t('auth.phoneInputTitle')}</Text>
                <View style={styles.headerSpacer} />
            </View>

            <View style={styles.content}>
                <Text style={styles.headline}>{t('auth.welcome')}</Text>
                <Text style={styles.subtitle}>{t('auth.phoneSubtitle')}</Text>
                <Card style={styles.card}>
                    <PhoneInput onSubmit={handleSubmit} submitting={submitting} />
                </Card>
            </View>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: colors.background,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        height: 56,
        paddingHorizontal: spacing.lg,
    },
    headerTitle: {
        ...typography.titleSmall,
        color: colors.text,
    },
    headerSpacer: {
        width: 24,
    },
    content: {
        flex: 1,
        justifyContent: 'center',
        paddingHorizontal: spacing.xl,
    },
    headline: {
        ...typography.titleLarge,
        color: colors.text,
        textAlign: 'center',
        marginBottom: spacing.sm,
    },
    subtitle: {
        ...typography.bodyLarge,
        color: colors.textMuted,
        textAlign: 'center',
        marginBottom: spacing.xxl,
    },
    card: {
        padding: spacing.xl,
    },
});

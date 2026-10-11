import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, StyleSheet, ScrollView, Alert, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { colors, typography, spacing } from '@/theme';
import { OtpInput } from '@/components/account';
import OutlineButton from '@/components/OutlineButton';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { selectUser } from '@/redux/selectors';
import { deleteAccount, sendOtp } from '@/redux/slices';
import { maskPhoneNumber } from '@/utils/phoneMask';

/** Account deletion requires a fresh server-side SMS OTP sent to the account's phone number. */
export default function DeleteAccountScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const dispatch = useAppDispatch();
    const user = useAppSelector(selectUser);
    const phone = user?.phoneNumber;

    const [sending, setSending] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [resetSignal, setResetSignal] = useState(0);

    const requestOtp = useCallback(async () => {
        if (!phone) return;
        setSending(true);
        setError(null);
        try {
            await dispatch(sendOtp({ phoneNumber: phone })).unwrap();
            setResetSignal((s) => s + 1);
        } catch {
            setError(t('auth.sendOtpError'));
        } finally {
            setSending(false);
        }
    }, [phone, dispatch, t]);

    useEffect(() => {
        // Deferred so the OTP request's state updates don't run synchronously inside the effect
        const id = setTimeout(() => void requestOtp(), 0);
        return () => clearTimeout(id);
    }, [requestOtp]);

    const handleComplete = useCallback(
        async (otp: string) => {
            setDeleting(true);
            setError(null);
            try {
                await dispatch(deleteAccount({ otp })).unwrap();
                Alert.alert('', t('account.deleteConfirm.success'), [
                    { text: t('common.ok'), onPress: () => router.back() },
                ]);
            } catch {
                setError(t('auth.invalidOtp'));
                setResetSignal((s) => s + 1);
            } finally {
                setDeleting(false);
            }
        },
        [dispatch, router, t],
    );

    return (
        <ScrollView style={styles.flex} contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
            <Text style={styles.body}>
                {t('account.deleteFlow.instructions', { phone: phone ? maskPhoneNumber(phone) : '' })}
            </Text>
            {sending ? (
                <ActivityIndicator color={colors.primary} />
            ) : (
                <OtpInput key={resetSignal} length={6} onComplete={handleComplete} disabled={deleting} error={!!error} />
            )}
            {error ? <Text style={styles.error}>{error}</Text> : null}
            {deleting ? <ActivityIndicator color={colors.error} /> : null}
            <OutlineButton
                title={t('auth.resendAvailable')}
                onPress={requestOtp}
                disabled={sending || deleting}
                variant="primary"
            />
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    flex: {
        flex: 1,
        backgroundColor: colors.background,
    },
    content: {
        padding: spacing.xl,
        gap: spacing.xl,
    },
    body: {
        ...typography.bodyLarge,
        color: colors.text,
        textAlign: 'center',
    },
    error: {
        ...typography.bodySmall,
        color: colors.error,
        textAlign: 'center',
    },
});

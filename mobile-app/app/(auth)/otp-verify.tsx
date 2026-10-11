import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { View, Text, StyleSheet, Alert, ActivityIndicator, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter, useLocalSearchParams } from 'expo-router';
import Ionicons from '@expo/vector-icons/Ionicons';
import { colors, typography, spacing, radius } from '@/theme';
import { useAppDispatch } from '@/redux/hooks';
import { firebaseVerify, persistSessionTokens, sendOtp, verifyOtp } from '@/redux/slices';
import { maskPhoneNumber } from '@/utils/phoneMask';
import {
    AUTH_PROVIDER,
    getAuth,
    getConfirmationResult,
    setConfirmationResult,
    signInWithPhoneNumber,
} from '@/services/firebase';
import { OtpInput } from '@/components/account';
import OutlineButton from '@/components/OutlineButton';

const RESEND_COOLDOWN_SEC = 60;

export default function OtpVerifyScreen() {
    const { t } = useTranslation();
    const dispatch = useAppDispatch();
    const router = useRouter();
    const { phone } = useLocalSearchParams<{ phone: string }>();

    const [resendCountdown, setResendCountdown] = useState(RESEND_COOLDOWN_SEC);
    const [resending, setResending] = useState(false);
    const [verifying, setVerifying] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [resetSignal, setResetSignal] = useState(0);
    const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);

    const startCountdownTimer = useCallback(() => {
        if (countdownRef.current) clearInterval(countdownRef.current);
        countdownRef.current = setInterval(() => {
            setResendCountdown((prev) => {
                if (prev <= 1) {
                    if (countdownRef.current) clearInterval(countdownRef.current);
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
    }, []);

    const startCountdown = useCallback(() => {
        setResendCountdown(RESEND_COOLDOWN_SEC);
        startCountdownTimer();
    }, [startCountdownTimer]);

    useEffect(() => {
        startCountdownTimer();
        return () => {
            if (countdownRef.current) clearInterval(countdownRef.current);
        };
    }, [startCountdownTimer]);

    const translateError = useCallback(
        (raw: string | undefined): string => {
            const lower = (raw ?? '').toLowerCase();
            if (lower.includes('expired')) return t('auth.otpExpired');
            if (lower.includes('attempt')) return t('auth.tooManyAttempts');
            return t('auth.invalidOtp');
        },
        [t],
    );

    const closeAuthFlow = useCallback(() => {
        // Pop back to login, then dismiss the (auth) modal itself.
        if (router.canDismiss()) router.dismissAll();
        router.back();
    }, [router]);

    const handleOtpComplete = useCallback(
        async (code: string) => {
            if (!phone || verifying) return;
            setErrorMessage(null);
            setVerifying(true);
            try {
                if (AUTH_PROVIDER === 'firebase') {
                    const confirmation = getConfirmationResult();
                    if (!confirmation) {
                        setErrorMessage(t('auth.otpExpired'));
                        return;
                    }
                    let idToken: string;
                    try {
                        const credential = await confirmation.confirm(code);
                        if (!credential) throw new Error('No credential');
                        idToken = await credential.user.getIdToken();
                    } catch (error: unknown) {
                        const errCode = (error as { code?: string } | null)?.code;
                        setErrorMessage(
                            errCode === 'auth/session-expired' ? t('auth.otpExpired') : t('auth.invalidOtp'),
                        );
                        return;
                    }
                    setConfirmationResult(null);
                    await dispatch(firebaseVerify({ idToken })).unwrap();
                } else {
                    await dispatch(verifyOtp({ phoneNumber: phone, otp: code })).unwrap();
                }
                await dispatch(persistSessionTokens());
                closeAuthFlow();
            } catch (error: unknown) {
                setErrorMessage(translateError(typeof error === 'string' ? error : undefined));
            } finally {
                setVerifying(false);
            }
        },
        [phone, verifying, dispatch, closeAuthFlow, translateError, t],
    );

    const handleResend = useCallback(async () => {
        if (resendCountdown > 0 || resending || !phone) return;
        setResending(true);
        try {
            if (AUTH_PROVIDER === 'firebase') {
                setConfirmationResult(await signInWithPhoneNumber(getAuth(), phone));
            } else {
                await dispatch(sendOtp({ phoneNumber: phone })).unwrap();
            }
            setErrorMessage(null);
            setResetSignal((s) => s + 1);
            startCountdown();
        } catch {
            Alert.alert('', t('auth.sendOtpError'));
        } finally {
            setResending(false);
        }
    }, [resendCountdown, resending, phone, dispatch, startCountdown, t]);

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <TouchableOpacity
                    onPress={() => router.back()}
                    accessibilityRole="button"
                    accessibilityLabel={t('common.back')}
                    hitSlop={12}
                >
                    <Ionicons name="chevron-back" size={24} color={colors.text} />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>{t('auth.otpTitle')}</Text>
                <View style={styles.headerSpacer} />
            </View>

            <View style={styles.content}>
                <Text style={styles.headline}>{t('auth.otpSubtitle')}</Text>
                <Text style={styles.subtitle}>
                    {t('auth.otpSentTo', { phone: phone ? maskPhoneNumber(phone) : '' })}
                </Text>

                <OtpInput
                    key={resetSignal}
                    length={6}
                    onComplete={handleOtpComplete}
                    disabled={verifying}
                    error={errorMessage !== null}
                />

                {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}

                <View style={styles.resendSection}>
                    <Text style={styles.noCodeText}>{t('auth.noCode')}</Text>
                    {resendCountdown > 0 ? (
                        <Text style={styles.countdownText}>
                            {t('auth.resendIn', { seconds: resendCountdown })}
                        </Text>
                    ) : (
                        <OutlineButton
                            title={t('auth.resendAvailable')}
                            onPress={handleResend}
                            disabled={resending}
                            variant="primary"
                        />
                    )}
                </View>
            </View>

            {verifying && (
                <View style={styles.loadingOverlay}>
                    <View style={styles.loadingCard}>
                        <ActivityIndicator size="large" color={colors.primary} />
                        <Text style={styles.loadingText}>{t('auth.verifying')}</Text>
                    </View>
                </View>
            )}
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
        alignItems: 'center',
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
        ...typography.bodyMedium,
        color: colors.textMuted,
        textAlign: 'center',
        marginBottom: spacing.xxl,
    },
    errorText: {
        ...typography.bodySmall,
        color: colors.error,
        textAlign: 'center',
        marginTop: spacing.md,
    },
    resendSection: {
        alignItems: 'center',
        marginTop: spacing.xxl,
        gap: spacing.sm,
    },
    noCodeText: {
        ...typography.caption,
        color: colors.textMuted,
    },
    countdownText: {
        ...typography.bodyMedium,
        color: colors.textSubtle,
    },
    loadingOverlay: {
        ...StyleSheet.absoluteFill,
        backgroundColor: colors.overlay,
        alignItems: 'center',
        justifyContent: 'center',
    },
    loadingCard: {
        backgroundColor: colors.surface,
        borderRadius: radius.xl,
        paddingVertical: spacing.xxl,
        paddingHorizontal: spacing.xxxl,
        alignItems: 'center',
        minWidth: 200,
    },
    loadingText: {
        ...typography.bodyLarge,
        color: colors.text,
        marginTop: spacing.lg,
    },
});

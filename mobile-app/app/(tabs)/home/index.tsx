import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
    View,
    Text,
    TextInput,
    StyleSheet,
    ScrollView,
    TouchableOpacity,
    KeyboardAvoidingView,
    Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { colors, typography, spacing, radius } from '@/theme';
import Card from '@/components/Card';
import PrimaryButton from '@/components/PrimaryButton';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { selectFeatures, selectJobsSubmitError, selectJobsSubmitStatus } from '@/redux/selectors';
import { clearSubmitError, submitEchoJob } from '@/redux/slices';
import { ECHO_DELAY_OPTIONS, ECHO_MESSAGE_MAX } from '@/types/job';

export default function HomeScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const dispatch = useAppDispatch();
    const features = useAppSelector(selectFeatures);
    const submitStatus = useAppSelector(selectJobsSubmitStatus);
    const submitError = useAppSelector(selectJobsSubmitError);

    const [message, setMessage] = useState('');
    const [delaySec, setDelaySec] = useState<number>(ECHO_DELAY_OPTIONS[1]);

    const trimmed = message.trim();
    const submitting = submitStatus === 'loading';

    const handleSubmit = useCallback(async () => {
        if (!trimmed || submitting) return;
        const action = await dispatch(submitEchoJob({ message: trimmed, delaySec }));
        if (submitEchoJob.fulfilled.match(action)) {
            dispatch(clearSubmitError());
            router.push({ pathname: '/(tabs)/home/job', params: { jobId: action.payload.jobId } });
        }
    }, [trimmed, submitting, delaySec, dispatch, router]);

    return (
        <KeyboardAvoidingView
            style={styles.flex}
            behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        >
            <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
                <Card style={styles.gap}>
                    <Text style={styles.title}>{t('home.welcomeTitle')}</Text>
                    <Text style={styles.body}>{t('home.welcomeBody')}</Text>
                </Card>

                {features.echo ? (
                    <Card style={styles.gap} testID="echo-demo">
                        <Text style={styles.sectionTitle}>{t('home.echo.title')}</Text>
                        <Text style={styles.body}>{t('home.echo.description')}</Text>

                        <Text style={styles.label}>{t('home.echo.messageLabel')}</Text>
                        <TextInput
                            style={styles.input}
                            value={message}
                            onChangeText={setMessage}
                            placeholder={t('home.echo.messagePlaceholder')}
                            placeholderTextColor={colors.textSubtle}
                            maxLength={ECHO_MESSAGE_MAX}
                            multiline
                            accessibilityLabel={t('home.echo.messageLabel')}
                            selectionColor={colors.primary}
                        />
                        <Text style={styles.counter}>
                            {t('home.echo.charCount', { count: message.length, max: ECHO_MESSAGE_MAX })}
                        </Text>

                        <Text style={styles.label}>{t('home.echo.delayLabel')}</Text>
                        <View style={styles.chips}>
                            {ECHO_DELAY_OPTIONS.map((option) => {
                                const selected = option === delaySec;
                                return (
                                    <TouchableOpacity
                                        key={option}
                                        style={[styles.chip, selected && styles.chipSelected]}
                                        onPress={() => setDelaySec(option)}
                                        accessibilityRole="button"
                                        accessibilityState={{ selected }}
                                    >
                                        <Text style={[styles.chipText, selected && styles.chipTextSelected]}>
                                            {t('home.echo.delaySeconds', { count: option })}
                                        </Text>
                                    </TouchableOpacity>
                                );
                            })}
                        </View>

                        {submitError ? <Text style={styles.error}>{submitError}</Text> : null}

                        <PrimaryButton
                            title={t('home.echo.submit')}
                            onPress={handleSubmit}
                            disabled={!trimmed}
                            loading={submitting}
                        />
                    </Card>
                ) : (
                    <Card>
                        <Text style={styles.body}>{t('home.echo.disabled')}</Text>
                    </Card>
                )}
            </ScrollView>
        </KeyboardAvoidingView>
    );
}

const styles = StyleSheet.create({
    flex: {
        flex: 1,
        backgroundColor: colors.background,
    },
    content: {
        padding: spacing.lg,
        gap: spacing.lg,
    },
    gap: {
        gap: spacing.md,
    },
    title: {
        ...typography.titleMedium,
        color: colors.text,
    },
    sectionTitle: {
        ...typography.titleSmall,
        color: colors.text,
    },
    body: {
        ...typography.bodyMedium,
        color: colors.textMuted,
    },
    label: {
        ...typography.label,
        color: colors.text,
        marginTop: spacing.sm,
    },
    input: {
        ...typography.bodyLarge,
        color: colors.text,
        backgroundColor: colors.surfaceMuted,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: colors.border,
        borderRadius: radius.md,
        padding: spacing.md,
        minHeight: 80,
        textAlignVertical: 'top',
    },
    counter: {
        ...typography.bodySmall,
        color: colors.textSubtle,
        textAlign: 'right',
    },
    chips: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: spacing.sm,
    },
    chip: {
        paddingVertical: spacing.sm,
        paddingHorizontal: spacing.lg,
        borderRadius: radius.full,
        borderWidth: 1,
        borderColor: colors.border,
    },
    chipSelected: {
        backgroundColor: colors.primaryMuted,
        borderColor: colors.primary,
    },
    chipText: {
        ...typography.label,
        color: colors.textMuted,
    },
    chipTextSelected: {
        color: colors.text,
    },
    error: {
        ...typography.bodySmall,
        color: colors.error,
    },
});

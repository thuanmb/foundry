import { useTranslation } from 'react-i18next';
import { View, Text, StyleSheet, ScrollView, ActivityIndicator } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { colors, typography, spacing } from '@/theme';
import Card from '@/components/Card';
import OutlineButton from '@/components/OutlineButton';
import { useAppSelector } from '@/redux/hooks';
import { selectJobById } from '@/redux/selectors';
import { isTerminalStatus } from '@/redux/slices';
import { useJobProgress } from '@/hooks/useJobProgress';

function Row({ label, value }: { label: string; value: string }) {
    return (
        <View style={styles.row}>
            <Text style={styles.rowLabel}>{label}</Text>
            <Text style={styles.rowValue} selectable>
                {value}
            </Text>
        </View>
    );
}

export default function JobScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const { jobId } = useLocalSearchParams<{ jobId: string }>();
    const job = useAppSelector(selectJobById(jobId));

    useJobProgress(jobId);

    const status = job?.status ?? 'PENDING';
    const done = isTerminalStatus(status);
    const statusColor =
        status === 'COMPLETED' ? colors.success : status === 'FAILED' ? colors.error : colors.warning;

    return (
        <ScrollView style={styles.flex} contentContainerStyle={styles.content}>
            <Card style={styles.gap}>
                <Text style={styles.meta} selectable>
                    {t('job.jobId', { id: jobId ?? '' })}
                </Text>
                <View style={styles.statusRow}>
                    {!done && <ActivityIndicator color={colors.primary} />}
                    <Text style={[styles.status, { color: statusColor }]} testID="job-status">
                        {t(`job.status.${status}`)}
                    </Text>
                </View>
                {!done && job?.estimatedWaitSec ? (
                    <Text style={styles.meta}>{t('job.estimatedWait', { seconds: job.estimatedWaitSec })}</Text>
                ) : null}
            </Card>

            {status === 'COMPLETED' && job?.result ? (
                <Card style={styles.gap}>
                    <Text style={styles.sectionTitle}>{t('job.result.title')}</Text>
                    <Row label={t('job.result.echo')} value={job.result.echo} />
                    <Row label={t('job.result.length')} value={String(job.result.length)} />
                    <Row
                        label={t('job.result.processedAt')}
                        value={new Date(job.result.processedAt).toLocaleString()}
                    />
                    {job.result.llmReply ? (
                        <Row label={t('job.result.llmReply')} value={job.result.llmReply} />
                    ) : null}
                </Card>
            ) : null}

            {status === 'FAILED' ? (
                <Card style={styles.gap}>
                    <Text style={[styles.sectionTitle, { color: colors.error }]}>{t('job.errorTitle')}</Text>
                    <Text style={styles.body}>{job?.error?.message ?? t('errors.generic')}</Text>
                </Card>
            ) : null}

            {done ? (
                <OutlineButton title={t('job.newJob')} variant="primary" onPress={() => router.back()} />
            ) : null}
        </ScrollView>
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
    meta: {
        ...typography.bodySmall,
        color: colors.textMuted,
    },
    statusRow: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: spacing.sm,
    },
    status: {
        ...typography.titleSmall,
    },
    sectionTitle: {
        ...typography.titleSmall,
        color: colors.text,
    },
    body: {
        ...typography.bodyMedium,
        color: colors.text,
    },
    row: {
        gap: spacing.xs,
    },
    rowLabel: {
        ...typography.caption,
        color: colors.textMuted,
    },
    rowValue: {
        ...typography.bodyLarge,
        color: colors.text,
    },
});

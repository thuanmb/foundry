import { useTranslation } from 'react-i18next';
import { Text, StyleSheet, ScrollView, View } from 'react-native';
import { colors, typography, spacing } from '@/theme';

const SECTIONS = ['collection', 'usage', 'storage', 'rights', 'contact'] as const;

export default function PrivacyScreen() {
    const { t } = useTranslation();

    return (
        <ScrollView style={styles.flex} contentContainerStyle={styles.content}>
            <Text style={styles.meta}>{t('account.privacy.lastUpdated')}</Text>
            <Text style={styles.body}>{t('account.privacy.intro')}</Text>
            {SECTIONS.map((key) => (
                <View key={key} style={styles.section}>
                    <Text style={styles.sectionTitle}>{t(`account.privacy.sections.${key}.title`)}</Text>
                    <Text style={styles.body}>{t(`account.privacy.sections.${key}.body`)}</Text>
                </View>
            ))}
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
    meta: {
        ...typography.bodySmall,
        color: colors.textSubtle,
    },
    section: {
        gap: spacing.xs,
    },
    sectionTitle: {
        ...typography.titleSmall,
        color: colors.text,
    },
    body: {
        ...typography.bodyMedium,
        color: colors.textMuted,
    },
});

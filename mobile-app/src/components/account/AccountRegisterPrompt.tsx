import { Text, StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { colors, typography, spacing } from '@/theme';
import Card from '@/components/Card';
import PrimaryButton from '@/components/PrimaryButton';
import OutlineButton from '@/components/OutlineButton';

interface AccountRegisterPromptProps {
    onRegister: () => void;
    onLogin: () => void;
}

export default function AccountRegisterPrompt({ onRegister, onLogin }: AccountRegisterPromptProps) {
    const { t } = useTranslation();

    return (
        <Card style={styles.card}>
            <Text style={styles.promptText}>{t('account.registerPrompt')}</Text>
            <PrimaryButton title={t('account.registerNow')} onPress={onRegister} style={styles.button} />
            <Text style={styles.loginPromptText}>{t('account.loginPrompt')}</Text>
            <OutlineButton title={t('account.loginNow')} onPress={onLogin} variant="primary" style={styles.button} />
        </Card>
    );
}

const styles = StyleSheet.create({
    card: {
        alignItems: 'center',
        marginHorizontal: spacing.lg,
        gap: spacing.md,
    },
    promptText: {
        ...typography.bodyLarge,
        color: colors.text,
        textAlign: 'center',
    },
    button: {
        alignSelf: 'stretch',
    },
    loginPromptText: {
        ...typography.bodyMedium,
        color: colors.textMuted,
        textAlign: 'center',
        marginTop: spacing.sm,
    },
});

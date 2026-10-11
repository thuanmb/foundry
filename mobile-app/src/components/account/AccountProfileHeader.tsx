import { View, Text, StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { colors, typography, spacing } from '@/theme';
import { maskPhoneNumber } from '@/utils/phoneMask';

interface AccountProfileHeaderProps {
    displayName?: string | null;
    phoneNumber?: string;
}

export default function AccountProfileHeader({ displayName, phoneNumber }: AccountProfileHeaderProps) {
    const { t } = useTranslation();

    const maskedPhone = phoneNumber ? maskPhoneNumber(phoneNumber) : null;
    const title = displayName || maskedPhone || t('account.guest');
    const initial = (displayName || phoneNumber?.slice(-1) || '?').charAt(0).toUpperCase();

    return (
        <View style={styles.container}>
            <View style={styles.avatar}>
                <Text style={styles.avatarInitial}>{initial}</Text>
            </View>
            <Text style={styles.title}>{title}</Text>
            {displayName && maskedPhone ? <Text style={styles.subtitle}>{maskedPhone}</Text> : null}
        </View>
    );
}

const AVATAR_SIZE = 80;

const styles = StyleSheet.create({
    container: {
        alignItems: 'center',
        paddingTop: spacing.xxl,
        paddingBottom: spacing.lg,
        gap: spacing.xs,
    },
    avatar: {
        width: AVATAR_SIZE,
        height: AVATAR_SIZE,
        borderRadius: AVATAR_SIZE / 2,
        backgroundColor: colors.primaryMuted,
        borderWidth: 2,
        borderColor: colors.primary,
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: spacing.md,
    },
    avatarInitial: {
        ...typography.titleLarge,
        color: colors.primary,
    },
    title: {
        ...typography.titleSmall,
        color: colors.text,
    },
    subtitle: {
        ...typography.bodyMedium,
        color: colors.textMuted,
    },
});

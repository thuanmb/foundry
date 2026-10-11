import { TouchableOpacity, Text, StyleSheet, View } from 'react-native';
import { colors, typography, spacing, radius } from '@/theme';

interface AccountMenuItemProps {
    icon: string;
    label: string;
    onPress?: () => void;
    variant?: 'default' | 'danger';
    rightContent?: React.ReactNode;
}

export default function AccountMenuItem({
    icon,
    label,
    onPress,
    variant = 'default',
    rightContent,
}: AccountMenuItemProps) {
    const isDanger = variant === 'danger';
    const textColor = isDanger ? colors.error : colors.text;
    const iconColor = isDanger ? colors.error : colors.textMuted;

    return (
        <TouchableOpacity
            style={styles.container}
            onPress={onPress}
            disabled={!onPress}
            activeOpacity={0.6}
            accessibilityRole="button"
            accessibilityLabel={label}
            accessibilityState={{ disabled: !onPress }}
        >
            <View style={styles.leftGroup}>
                <Text style={[styles.icon, { color: iconColor }]}>{icon}</Text>
                <Text style={[styles.label, { color: textColor }]}>{label}</Text>
            </View>
            <View style={styles.rightGroup}>
                {rightContent ?? (
                    onPress ? (
                        <Text style={styles.chevron}>›</Text>
                    ) : null
                )}
            </View>
        </TouchableOpacity>
    );
}

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: spacing.lg,
        paddingHorizontal: spacing.xl,
        backgroundColor: colors.surface,
        borderRadius: radius.lg,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: colors.border,
        minHeight: 56,
    },
    leftGroup: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: spacing.lg,
    },
    icon: {
        fontSize: 22,
    },
    label: {
        ...typography.bodyLarge,
    },
    rightGroup: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    chevron: {
        fontSize: 24,
        color: colors.textSubtle,
    },
});

import {
    TouchableOpacity,
    Text,
    StyleSheet,
    ActivityIndicator,
    type TouchableOpacityProps,
} from 'react-native';
import { colors, typography, radius, spacing } from '@/theme';

interface PrimaryButtonProps extends Omit<TouchableOpacityProps, 'disabled'> {
    title: string;
    loading?: boolean;
    disabled?: boolean;
}

export default function PrimaryButton({
    title,
    loading = false,
    disabled = false,
    style,
    ...props
}: PrimaryButtonProps) {
    const isDisabled = disabled || loading;

    return (
        <TouchableOpacity
            style={[styles.button, isDisabled && styles.disabled, style]}
            disabled={isDisabled}
            activeOpacity={0.8}
            accessibilityRole="button"
            accessibilityLabel={title}
            accessibilityState={{ disabled: isDisabled, busy: loading }}
            {...props}
        >
            {loading ? (
                <ActivityIndicator color={colors.onPrimary} size="small" />
            ) : (
                <Text style={styles.label}>{title}</Text>
            )}
        </TouchableOpacity>
    );
}

const styles = StyleSheet.create({
    button: {
        backgroundColor: colors.primary,
        borderRadius: radius.md,
        paddingVertical: spacing.md,
        paddingHorizontal: spacing.xl,
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: 48,
    },
    disabled: {
        opacity: 0.4,
    },
    label: {
        ...typography.button,
        color: colors.onPrimary,
    },
});

import {
    TouchableOpacity,
    Text,
    StyleSheet,
    type TouchableOpacityProps,
} from 'react-native';
import { colors, typography, radius, spacing } from '@/theme';

interface OutlineButtonProps extends Omit<TouchableOpacityProps, 'disabled'> {
    title: string;
    variant?: 'primary' | 'default' | 'danger';
    disabled?: boolean;
}

export default function OutlineButton({
    title,
    variant = 'default',
    disabled = false,
    style,
    ...props
}: OutlineButtonProps) {
    const tint =
        variant === 'primary' ? colors.primary : variant === 'danger' ? colors.error : colors.text;
    const borderColor = variant === 'default' ? colors.border : tint;

    return (
        <TouchableOpacity
            style={[
                styles.button,
                { borderColor },
                disabled && styles.disabled,
                style,
            ]}
            disabled={disabled}
            activeOpacity={0.7}
            accessibilityRole="button"
            accessibilityLabel={title}
            accessibilityState={{ disabled }}
            {...props}
        >
            <Text
                style={[
                    styles.label,
                    { color: tint },
                ]}
            >
                {title}
            </Text>
        </TouchableOpacity>
    );
}

const styles = StyleSheet.create({
    button: {
        backgroundColor: 'transparent',
        borderWidth: 1,
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
    },
});

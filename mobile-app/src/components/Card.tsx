import { View, StyleSheet, type ViewProps } from 'react-native';
import { colors, radius, spacing } from '@/theme';

interface CardProps extends ViewProps {
    children: React.ReactNode;
}

export default function Card({ children, style, ...props }: CardProps) {
    return (
        <View style={[styles.card, style]} {...props}>
            {children}
        </View>
    );
}

const styles = StyleSheet.create({
    card: {
        backgroundColor: colors.surface,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: colors.border,
        borderRadius: radius.lg,
        padding: spacing.lg,
    },
});

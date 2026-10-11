import { useCallback, useRef, useState, useEffect } from 'react';
import { View, TextInput, StyleSheet } from 'react-native';
import { colors, typography, spacing, radius } from '@/theme';

interface OtpInputProps {
    length?: number;
    onComplete: (code: string) => void;
    disabled?: boolean;
    error?: boolean;
}

export default function OtpInput({
    length = 6,
    onComplete,
    disabled = false,
    error = false,
}: OtpInputProps) {
    const [digits, setDigits] = useState<string[]>(Array(length).fill(''));
    const inputRefs = useRef<(TextInput | null)[]>(Array(length).fill(null));

    // Focus first input on mount
    useEffect(() => {
        const timer = setTimeout(() => {
            inputRefs.current[0]?.focus();
        }, 300);
        return () => clearTimeout(timer);
    }, []);

    const handleChange = useCallback(
        (index: number, text: string) => {
            if (disabled) return;

            const cleaned = text.replace(/\D/g, '');
            if (cleaned.length === 0) {
                const newDigits = [...digits];
                newDigits[index] = '';
                setDigits(newDigits);
                return;
            }

            // Take only the last digit
            const digit = cleaned.slice(-1);
            const newDigits = [...digits];
            newDigits[index] = digit;
            setDigits(newDigits);

            // Auto-advance to next input
            if (index < length - 1) {
                inputRefs.current[index + 1]?.focus();
            } else {
                // All digits filled — call onComplete
                const code = newDigits.join('');
                if (code.length === length) {
                    onComplete(code);
                }
            }
        },
        [digits, disabled, length, onComplete],
    );

    const handleKeyPress = useCallback(
        (index: number, key: string) => {
            if (key === 'Backspace' && digits[index] === '' && index > 0) {
                // Move to previous input on backspace when current is empty
                const newDigits = [...digits];
                newDigits[index - 1] = '';
                setDigits(newDigits);
                inputRefs.current[index - 1]?.focus();
            }
        },
        [digits],
    );

    const borderColor = error ? colors.error : colors.primary;

    return (
        <View style={styles.container}>
            {digits.map((digit, index) => (
                <TextInput
                    key={index}
                    ref={(ref) => {
                        inputRefs.current[index] = ref;
                    }}
                    style={[
                        styles.input,
                        digit !== '' && styles.inputFilled,
                        { borderColor: digit !== '' ? borderColor : colors.border },
                        error && styles.inputError,
                    ]}
                    value={digit}
                    onChangeText={(text) => handleChange(index, text)}
                    onKeyPress={({ nativeEvent }) =>
                        handleKeyPress(index, nativeEvent.key)
                    }
                    keyboardType="number-pad"
                    maxLength={1}
                    selectTextOnFocus
                    editable={!disabled}
                    accessibilityLabel={`OTP digit ${index + 1}`}
                    testID={`otp-input-${index}`}
                    accessibilityRole="none"
                    selectionColor={colors.primary}
                />
            ))}
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        justifyContent: 'center',
        gap: spacing.sm,
    },
    input: {
        width: 48,
        height: 56,
        backgroundColor: colors.surfaceMuted,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.lg,
        textAlign: 'center',
        ...typography.titleMedium,
        color: colors.text,
    },
    inputFilled: {
        borderColor: colors.primary,
        backgroundColor: colors.primaryMuted,
    },
    inputError: {
        borderColor: colors.error,
    },
});

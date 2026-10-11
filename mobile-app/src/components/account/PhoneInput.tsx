import { useCallback, useState, useRef } from 'react';
import {
    View,
    Text,
    TextInput,
    StyleSheet,
    TouchableOpacity,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import { useRouter } from 'expo-router';
import type { Href } from 'expo-router';
import { colors, typography, spacing, radius } from '@/theme';
import PrimaryButton from '@/components/PrimaryButton';
import VietnamFlag from './VietnamFlag';

interface PhoneInputProps {
    onSubmit: (phoneNumber: string) => void;
    submitting: boolean;
}

export default function PhoneInput({ onSubmit, submitting }: PhoneInputProps) {
    const { t } = useTranslation();
    const router = useRouter();
    const [rawInput, setRawInput] = useState('');
    const inputRef = useRef<TextInput>(null);

    const digitsOnly = rawInput.replace(/\D/g, '');
    const isValid = digitsOnly.length >= 9;

    const handleChangeText = useCallback((text: string) => {
        // Remove any non-digit characters
        const digits = text.replace(/\D/g, '');
        // Limit to 10 digits for VN phone numbers (after +84, actual numbers are 9-10 digits)
        const truncated = digits.slice(0, 10);
        setRawInput(truncated);
    }, []);

    const handleSubmit = useCallback(() => {
        if (isValid && !submitting) {
            const phoneNumber = '+84' + digitsOnly;
            onSubmit(phoneNumber);
        }
    }, [digitsOnly, isValid, submitting, onSubmit]);

    return (
        <View style={styles.form}>
            <View style={styles.inputGroup}>
                <Text style={styles.label}>{t('auth.phoneLabel')}</Text>
                <View style={styles.phoneRow}>
                    <View style={styles.countryPrefix}>
                        <VietnamFlag size={24} />
                        <Text style={styles.prefixText}>+84</Text>
                    </View>
                    <TextInput
                        ref={inputRef}
                        style={styles.input}
                        value={rawInput}
                        onChangeText={handleChangeText}
                        placeholder="000 000 000"
                        placeholderTextColor={colors.textSubtle}
                        keyboardType="phone-pad"
                        maxLength={10}
                        returnKeyType="done"
                        onSubmitEditing={handleSubmit}
                        accessibilityLabel={t('auth.phoneLabel')}
                        accessibilityRole="none"
                        autoFocus
                        selectionColor={colors.primary}
                    />
                </View>
            </View>

            <PrimaryButton
                title={t('auth.continue')}
                onPress={handleSubmit}
                disabled={!isValid}
                loading={submitting}
            />

            <TouchableOpacity
                onPress={() => router.push('/(tabs)/account/privacy' as Href)}
                accessibilityRole="link"
                accessibilityLabel={t('account.privacy.title')}
                hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
                <Text style={styles.terms}>
                    {t('auth.termsNotice')}
                </Text>
            </TouchableOpacity>
        </View>
    );
}

const styles = StyleSheet.create({
    form: {
        gap: spacing.xl,
    },
    inputGroup: {
        gap: spacing.sm,
    },
    label: {
        ...typography.label,
        color: colors.textMuted,
    },
    phoneRow: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: colors.surfaceMuted,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: colors.border,
        borderRadius: radius.lg,
        height: 56,
        overflow: 'hidden',
    },
    countryPrefix: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: spacing.sm,
        paddingHorizontal: spacing.lg,
        paddingRight: spacing.md,
        borderRightWidth: StyleSheet.hairlineWidth,
        borderRightColor: colors.border,
        height: '100%',
    },
    prefixText: {
        ...typography.bodyLarge,
        color: colors.text,
    },
    input: {
        flex: 1,
        height: '100%',
        color: colors.text,
        fontSize: 20,
        letterSpacing: 1,
        paddingHorizontal: spacing.lg,
    },
    terms: {
        ...typography.bodySmall,
        color: colors.textMuted,
        textAlign: 'center',
        lineHeight: 20,
    },
});

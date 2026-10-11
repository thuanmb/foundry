import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Text, TextInput, StyleSheet, ScrollView, Alert } from 'react-native';
import { useRouter } from 'expo-router';
import { colors, typography, spacing, radius } from '@/theme';
import PrimaryButton from '@/components/PrimaryButton';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { selectProfileError, selectProfileStatus, selectUser } from '@/redux/selectors';
import { updateProfile } from '@/redux/slices';

const DISPLAY_NAME_MAX = 100;

export default function EditProfileScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const dispatch = useAppDispatch();
    const user = useAppSelector(selectUser);
    const status = useAppSelector(selectProfileStatus);
    const serverError = useAppSelector(selectProfileError);

    const [displayName, setDisplayName] = useState(user?.displayName ?? '');
    const [validationError, setValidationError] = useState<string | null>(null);

    const handleSave = useCallback(async () => {
        const trimmed = displayName.trim();
        if (!trimmed) {
            setValidationError(t('account.editProfile.required'));
            return;
        }
        setValidationError(null);
        const action = await dispatch(updateProfile({ displayName: trimmed }));
        if (updateProfile.fulfilled.match(action)) {
            Alert.alert(t('account.editProfile.successTitle'), t('account.editProfile.successMessage'), [
                { text: t('common.ok'), onPress: () => router.back() },
            ]);
        }
    }, [displayName, dispatch, router, t]);

    const error = validationError ?? (status === 'failed' ? serverError : null);

    return (
        <ScrollView style={styles.flex} contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
            <Text style={styles.label}>{t('account.editProfile.displayNameLabel')}</Text>
            <TextInput
                style={[styles.input, error ? styles.inputError : null]}
                value={displayName}
                onChangeText={setDisplayName}
                placeholder={t('account.editProfile.displayNamePlaceholder')}
                placeholderTextColor={colors.textSubtle}
                maxLength={DISPLAY_NAME_MAX}
                autoFocus
                returnKeyType="done"
                onSubmitEditing={handleSave}
                accessibilityLabel={t('account.editProfile.displayNameLabel')}
                selectionColor={colors.primary}
            />
            {error ? <Text style={styles.error}>{error}</Text> : null}
            <PrimaryButton title={t('common.save')} onPress={handleSave} loading={status === 'loading'} />
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
        gap: spacing.md,
    },
    label: {
        ...typography.label,
        color: colors.text,
    },
    input: {
        ...typography.bodyLarge,
        color: colors.text,
        backgroundColor: colors.surfaceMuted,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: colors.border,
        borderRadius: radius.md,
        padding: spacing.md,
    },
    inputError: {
        borderColor: colors.error,
    },
    error: {
        ...typography.bodySmall,
        color: colors.error,
    },
});

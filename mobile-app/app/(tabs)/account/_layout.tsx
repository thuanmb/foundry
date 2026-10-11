import { Stack } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { stackScreenOptions } from '@/theme/navigation';

export default function AccountLayout() {
    const { t } = useTranslation();
    return (
        <Stack screenOptions={stackScreenOptions}>
            <Stack.Screen name="index" options={{ title: t('account.title') }} />
            <Stack.Screen name="edit-profile" options={{ title: t('account.editProfile.title') }} />
            <Stack.Screen name="privacy" options={{ title: t('account.privacy.title') }} />
            <Stack.Screen name="delete-account" options={{ title: t('account.deleteFlow.title') }} />
        </Stack>
    );
}

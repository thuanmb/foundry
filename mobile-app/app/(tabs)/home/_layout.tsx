import { Stack } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { stackScreenOptions } from '@/theme/navigation';

export default function HomeLayout() {
    const { t } = useTranslation();
    return (
        <Stack screenOptions={stackScreenOptions}>
            <Stack.Screen name="index" options={{ title: t('home.title') }} />
            <Stack.Screen name="job" options={{ title: t('job.title') }} />
        </Stack>
    );
}

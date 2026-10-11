import { Stack } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { stackScreenOptions } from '@/theme/navigation';

export default function NotesLayout() {
    const { t } = useTranslation();
    return (
        <Stack screenOptions={stackScreenOptions}>
            <Stack.Screen name="index" options={{ title: t('notes.title') }} />
            <Stack.Screen name="edit" options={{ title: t('notes.editNote') }} />
        </Stack>
    );
}

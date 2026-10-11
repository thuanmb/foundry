import { useCallback, useEffect, useLayoutEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
    View,
    Text,
    TextInput,
    StyleSheet,
    ScrollView,
    Alert,
    KeyboardAvoidingView,
    Platform,
    ActivityIndicator,
} from 'react-native';
import { useLocalSearchParams, useNavigation, useRouter } from 'expo-router';
import { colors, typography, spacing, radius } from '@/theme';
import PrimaryButton from '@/components/PrimaryButton';
import OutlineButton from '@/components/OutlineButton';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { selectNoteById, selectNotesState } from '@/redux/selectors';
import { createNote, deleteNote, resetSaveStatus, updateNote } from '@/redux/slices';
import { getNote } from '@/services/notesApi';
import { getErrorMessage } from '@/utils/apiError';
import { NOTE_CONTENT_MAX, NOTE_TITLE_MAX } from '@/types/note';

export default function NoteEditScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const navigation = useNavigation();
    const dispatch = useAppDispatch();
    const { id } = useLocalSearchParams<{ id?: string }>();
    const isEdit = Boolean(id);
    const storedNote = useAppSelector(selectNoteById(id));
    const { saveStatus, saveError } = useAppSelector(selectNotesState);

    const [title, setTitle] = useState(storedNote?.title ?? '');
    const [content, setContent] = useState(storedNote?.content ?? '');
    const [loading, setLoading] = useState(isEdit && !storedNote);
    const [titleError, setTitleError] = useState<string | null>(null);

    const saving = saveStatus === 'loading';

    useLayoutEffect(() => {
        navigation.setOptions({ title: isEdit ? t('notes.editNote') : t('notes.newNote') });
    }, [navigation, isEdit, t]);

    useEffect(() => {
        dispatch(resetSaveStatus());
    }, [dispatch]);

    // Deep link / cold start: the note may not be in the list yet.
    useEffect(() => {
        if (!id || storedNote) return;
        let cancelled = false;
        getNote(id)
            .then((note) => {
                if (cancelled) return;
                setTitle(note.title);
                setContent(note.content ?? '');
            })
            .catch((error) => {
                if (!cancelled) Alert.alert('', getErrorMessage(error, t('errors.notFound')));
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });
        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    const handleSave = useCallback(async () => {
        const input = { title: title.trim(), content };
        if (!input.title) {
            setTitleError(t('notes.titleRequired'));
            return;
        }
        setTitleError(null);
        const action = id
            ? await dispatch(updateNote({ id, input }))
            : await dispatch(createNote(input));
        if (createNote.fulfilled.match(action) || updateNote.fulfilled.match(action)) {
            router.back();
        }
    }, [title, content, id, dispatch, router, t]);

    const handleDelete = useCallback(() => {
        if (!id) return;
        Alert.alert(t('notes.deleteConfirm.title'), t('notes.deleteConfirm.message'), [
            { text: t('common.cancel'), style: 'cancel' },
            {
                text: t('common.delete'),
                style: 'destructive',
                onPress: async () => {
                    const action = await dispatch(deleteNote(id));
                    if (deleteNote.fulfilled.match(action)) router.back();
                },
            },
        ]);
    }, [id, dispatch, router, t]);

    if (loading) {
        return (
            <View style={styles.centered}>
                <ActivityIndicator color={colors.primary} size="large" />
            </View>
        );
    }

    return (
        <KeyboardAvoidingView
            style={styles.flex}
            behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        >
            <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
                <Text style={styles.label}>{t('notes.titleLabel')}</Text>
                <TextInput
                    style={[styles.input, titleError ? styles.inputError : null]}
                    value={title}
                    onChangeText={setTitle}
                    placeholder={t('notes.titlePlaceholder')}
                    placeholderTextColor={colors.textSubtle}
                    maxLength={NOTE_TITLE_MAX}
                    accessibilityLabel={t('notes.titleLabel')}
                    selectionColor={colors.primary}
                />
                {titleError ? <Text style={styles.error}>{titleError}</Text> : null}

                <Text style={styles.label}>{t('notes.contentLabel')}</Text>
                <TextInput
                    style={[styles.input, styles.contentInput]}
                    value={content}
                    onChangeText={setContent}
                    placeholder={t('notes.contentPlaceholder')}
                    placeholderTextColor={colors.textSubtle}
                    maxLength={NOTE_CONTENT_MAX}
                    multiline
                    accessibilityLabel={t('notes.contentLabel')}
                    selectionColor={colors.primary}
                />

                {saveStatus === 'failed' && saveError ? <Text style={styles.error}>{saveError}</Text> : null}

                <PrimaryButton title={t('common.save')} onPress={handleSave} loading={saving} />
                {isEdit ? (
                    <OutlineButton
                        title={t('common.delete')}
                        variant="danger"
                        onPress={handleDelete}
                        disabled={saving}
                    />
                ) : null}
            </ScrollView>
        </KeyboardAvoidingView>
    );
}

const styles = StyleSheet.create({
    flex: {
        flex: 1,
        backgroundColor: colors.background,
    },
    centered: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
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
    contentInput: {
        minHeight: 200,
        textAlignVertical: 'top',
    },
    error: {
        ...typography.bodySmall,
        color: colors.error,
    },
});

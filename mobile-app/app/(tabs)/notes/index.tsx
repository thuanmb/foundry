import { useCallback, useEffect, useLayoutEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
    View,
    Text,
    StyleSheet,
    FlatList,
    RefreshControl,
    TouchableOpacity,
    ActivityIndicator,
} from 'react-native';
import { useNavigation, useRouter } from 'expo-router';
import Ionicons from '@expo/vector-icons/Ionicons';
import { colors, typography, spacing } from '@/theme';
import Card from '@/components/Card';
import PrimaryButton from '@/components/PrimaryButton';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { selectFeatures, selectIsAuthenticated, selectNotesState } from '@/redux/selectors';
import { fetchNotes } from '@/redux/slices';
import type { Note } from '@/types/note';

function NoteRow({ note, onPress }: { note: Note; onPress: () => void }) {
    const { t } = useTranslation();
    return (
        <TouchableOpacity onPress={onPress} activeOpacity={0.7} accessibilityRole="button">
            <Card style={styles.noteCard}>
                <Text style={styles.noteTitle} numberOfLines={1}>
                    {note.title}
                </Text>
                {note.content ? (
                    <Text style={styles.noteContent} numberOfLines={2}>
                        {note.content}
                    </Text>
                ) : null}
                <Text style={styles.noteMeta}>
                    {t('notes.updatedAt', { date: new Date(note.updatedAt).toLocaleString() })}
                </Text>
            </Card>
        </TouchableOpacity>
    );
}

export default function NotesScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const navigation = useNavigation();
    const dispatch = useAppDispatch();
    const isAuthenticated = useAppSelector(selectIsAuthenticated);
    const features = useAppSelector(selectFeatures);
    const { items, page, totalPages, listStatus, listError } = useAppSelector(selectNotesState);

    const enabled = isAuthenticated && features.notes;

    const openEditor = useCallback(
        (id?: string) => router.push({ pathname: '/(tabs)/notes/edit', params: id ? { id } : {} }),
        [router],
    );

    useLayoutEffect(() => {
        navigation.setOptions({
            headerRight: enabled
                ? () => (
                    <TouchableOpacity
                        onPress={() => openEditor()}
                        accessibilityRole="button"
                        accessibilityLabel={t('notes.newNote')}
                        hitSlop={12}
                    >
                        <Ionicons name="add" size={28} color={colors.primary} />
                    </TouchableOpacity>
                )
                : undefined,
        });
    }, [navigation, enabled, openEditor, t]);

    useEffect(() => {
        if (enabled) dispatch(fetchNotes({ page: 0 }));
    }, [enabled, dispatch]);

    const handleRefresh = useCallback(() => {
        dispatch(fetchNotes({ page: 0, refresh: true }));
    }, [dispatch]);

    const handleEndReached = useCallback(() => {
        if (listStatus === 'succeeded' && page + 1 < totalPages) {
            dispatch(fetchNotes({ page: page + 1 }));
        }
    }, [listStatus, page, totalPages, dispatch]);

    if (!features.notes) {
        return (
            <View style={styles.centered}>
                <Text style={styles.message}>{t('notes.disabled')}</Text>
            </View>
        );
    }

    if (!isAuthenticated) {
        return (
            <View style={styles.centered}>
                <Ionicons name="lock-closed-outline" size={48} color={colors.textMuted} />
                <Text style={styles.message}>{t('notes.loginRequired')}</Text>
                <PrimaryButton
                    title={t('notes.loginCta')}
                    onPress={() => router.push('/(auth)/login')}
                    style={styles.cta}
                />
            </View>
        );
    }

    if (listStatus === 'loading' && items.length === 0) {
        return (
            <View style={styles.centered}>
                <ActivityIndicator color={colors.primary} size="large" />
            </View>
        );
    }

    return (
        <FlatList
            style={styles.list}
            contentContainerStyle={items.length === 0 ? styles.emptyContainer : styles.listContent}
            data={items}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => <NoteRow note={item} onPress={() => openEditor(item.id)} />}
            onEndReached={handleEndReached}
            onEndReachedThreshold={0.5}
            refreshControl={
                <RefreshControl
                    refreshing={listStatus === 'refreshing'}
                    onRefresh={handleRefresh}
                    tintColor={colors.primary}
                />
            }
            ListEmptyComponent={
                <View style={styles.centered}>
                    <Text style={styles.message}>
                        {listStatus === 'failed' ? (listError ?? t('errors.generic')) : t('notes.empty')}
                    </Text>
                    {listStatus === 'failed' ? (
                        <PrimaryButton title={t('common.retry')} onPress={handleRefresh} style={styles.cta} />
                    ) : (
                        <PrimaryButton title={t('notes.newNote')} onPress={() => openEditor()} style={styles.cta} />
                    )}
                </View>
            }
            ListFooterComponent={
                listStatus === 'loadingMore' ? (
                    <ActivityIndicator color={colors.primary} style={styles.footer} />
                ) : null
            }
        />
    );
}

const styles = StyleSheet.create({
    list: {
        flex: 1,
        backgroundColor: colors.background,
    },
    listContent: {
        padding: spacing.lg,
        gap: spacing.md,
    },
    emptyContainer: {
        flexGrow: 1,
    },
    centered: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        padding: spacing.xl,
        gap: spacing.lg,
        backgroundColor: colors.background,
    },
    message: {
        ...typography.bodyLarge,
        color: colors.textMuted,
        textAlign: 'center',
    },
    cta: {
        alignSelf: 'stretch',
    },
    noteCard: {
        gap: spacing.xs,
    },
    noteTitle: {
        ...typography.titleSmall,
        color: colors.text,
    },
    noteContent: {
        ...typography.bodyMedium,
        color: colors.textMuted,
    },
    noteMeta: {
        ...typography.bodySmall,
        color: colors.textSubtle,
    },
    footer: {
        paddingVertical: spacing.lg,
    },
});

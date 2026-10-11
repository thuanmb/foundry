import { configureStore } from '@reduxjs/toolkit';
import notesReducer, { createNote, deleteNote, fetchNotes, updateNote } from '../notesSlice';
import { clearAuth } from '../authSlice';
import * as notesApi from '@/services/notesApi';
import type { Note, PageResponse } from '@/types/note';

jest.mock('@/services/api', () => ({
    __esModule: true,
    default: {},
    storeAuthTokens: jest.fn(),
    clearAuthTokens: jest.fn(),
}));
jest.mock('@/services/notesApi');

const mockedApi = notesApi as jest.Mocked<typeof notesApi>;

function note(id: string, title = `Note ${id}`): Note {
    return { id, title, content: '', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' };
}

function page(content: Note[], pageNumber: number, totalPages: number): PageResponse<Note> {
    return { content, page: pageNumber, size: 20, totalElements: totalPages * 20, totalPages };
}

function makeStore() {
    return configureStore({ reducer: { notes: notesReducer } });
}

describe('notesSlice', () => {
    beforeEach(() => jest.resetAllMocks());

    it('replaces items on page 0 and appends de-duplicated items on later pages', async () => {
        const store = makeStore();
        mockedApi.listNotes.mockResolvedValueOnce(page([note('a'), note('b')], 0, 2));
        await store.dispatch(fetchNotes({ page: 0 }));
        expect(store.getState().notes.items.map((n) => n.id)).toEqual(['a', 'b']);
        expect(store.getState().notes.listStatus).toBe('succeeded');

        mockedApi.listNotes.mockResolvedValueOnce(page([note('b'), note('c')], 1, 2));
        await store.dispatch(fetchNotes({ page: 1 }));
        const state = store.getState().notes;
        expect(state.items.map((n) => n.id)).toEqual(['a', 'b', 'c']);
        expect(state.page).toBe(1);
        expect(state.totalPages).toBe(2);
    });

    it('sets listStatus by request kind', () => {
        const state = notesReducer(undefined, {
            type: fetchNotes.pending.type,
            meta: { arg: { page: 0, refresh: true } },
        });
        expect(state.listStatus).toBe('refreshing');
        const more = notesReducer(undefined, { type: fetchNotes.pending.type, meta: { arg: { page: 2 } } });
        expect(more.listStatus).toBe('loadingMore');
    });

    it('records list errors', async () => {
        const store = makeStore();
        mockedApi.listNotes.mockRejectedValueOnce({ response: { data: { detail: 'Boom' } } });
        await store.dispatch(fetchNotes({ page: 0 }));
        expect(store.getState().notes.listStatus).toBe('failed');
        expect(store.getState().notes.listError).toBe('Boom');
    });

    it('creates, updates and deletes notes', async () => {
        const store = makeStore();
        mockedApi.createNote.mockResolvedValueOnce(note('x', 'First'));
        await store.dispatch(createNote({ title: 'First', content: '' }));
        expect(store.getState().notes.items[0].title).toBe('First');
        expect(store.getState().notes.totalElements).toBe(1);

        mockedApi.updateNote.mockResolvedValueOnce(note('x', 'Renamed'));
        await store.dispatch(updateNote({ id: 'x', input: { title: 'Renamed', content: '' } }));
        expect(store.getState().notes.items[0].title).toBe('Renamed');
        expect(store.getState().notes.saveStatus).toBe('succeeded');

        mockedApi.deleteNote.mockResolvedValueOnce();
        await store.dispatch(deleteNote('x'));
        expect(store.getState().notes.items).toHaveLength(0);
        expect(store.getState().notes.totalElements).toBe(0);
    });

    it('marks save failures', async () => {
        const store = makeStore();
        mockedApi.createNote.mockRejectedValueOnce(new Error('Network Error'));
        await store.dispatch(createNote({ title: 'T', content: '' }));
        expect(store.getState().notes.saveStatus).toBe('failed');
        expect(store.getState().notes.saveError).toBe('Network Error');
    });

    it('resets on clearAuth', async () => {
        const store = makeStore();
        mockedApi.listNotes.mockResolvedValueOnce(page([note('a')], 0, 1));
        await store.dispatch(fetchNotes({ page: 0 }));
        store.dispatch(clearAuth());
        expect(store.getState().notes.items).toHaveLength(0);
        expect(store.getState().notes.page).toBe(-1);
    });
});

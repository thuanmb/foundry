import { createAsyncThunk, createSlice, isFulfilled, isPending, isRejected } from '@reduxjs/toolkit';
import * as notesApi from '@/services/notesApi';
import { getErrorMessage } from '@/utils/apiError';
import { clearAuth } from './authSlice';
import type { Note, NoteInput, PageResponse } from '@/types/note';

export const NOTES_PAGE_SIZE = 20;

export type NotesListStatus = 'idle' | 'loading' | 'refreshing' | 'loadingMore' | 'succeeded' | 'failed';

export interface NotesState {
    items: Note[];
    page: number;
    totalPages: number;
    totalElements: number;
    listStatus: NotesListStatus;
    listError: string | null;
    saveStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
    saveError: string | null;
}

const initialState: NotesState = {
    items: [],
    page: -1,
    totalPages: 0,
    totalElements: 0,
    listStatus: 'idle',
    listError: null,
    saveStatus: 'idle',
    saveError: null,
};

export const fetchNotes = createAsyncThunk<
    PageResponse<Note>,
    { page: number; refresh?: boolean },
    { rejectValue: string }
>('notes/fetch', async ({ page }, { rejectWithValue }) => {
    try {
        return await notesApi.listNotes(page, NOTES_PAGE_SIZE);
    } catch (error) {
        return rejectWithValue(getErrorMessage(error, 'Failed to load notes'));
    }
});

export const createNote = createAsyncThunk<Note, NoteInput, { rejectValue: string }>(
    'notes/create',
    async (input, { rejectWithValue }) => {
        try {
            return await notesApi.createNote(input);
        } catch (error) {
            return rejectWithValue(getErrorMessage(error, 'Failed to save note'));
        }
    },
);

export const updateNote = createAsyncThunk<Note, { id: string; input: NoteInput }, { rejectValue: string }>(
    'notes/update',
    async ({ id, input }, { rejectWithValue }) => {
        try {
            return await notesApi.updateNote(id, input);
        } catch (error) {
            return rejectWithValue(getErrorMessage(error, 'Failed to save note'));
        }
    },
);

export const deleteNote = createAsyncThunk<string, string, { rejectValue: string }>(
    'notes/delete',
    async (id, { rejectWithValue }) => {
        try {
            await notesApi.deleteNote(id);
            return id;
        } catch (error) {
            return rejectWithValue(getErrorMessage(error, 'Failed to delete note'));
        }
    },
);

const notesSlice = createSlice({
    name: 'notes',
    initialState,
    reducers: {
        resetSaveStatus(state) {
            state.saveStatus = 'idle';
            state.saveError = null;
        },
    },
    extraReducers: (builder) => {
        builder
            .addCase(clearAuth, () => initialState)
            .addCase(fetchNotes.pending, (state, action) => {
                const { page, refresh } = action.meta.arg;
                state.listStatus = refresh ? 'refreshing' : page > 0 ? 'loadingMore' : 'loading';
                state.listError = null;
            })
            .addCase(fetchNotes.fulfilled, (state, action) => {
                const { content, page, totalPages, totalElements } = action.payload;
                if (page === 0) {
                    state.items = content;
                } else {
                    const known = new Set(state.items.map((n) => n.id));
                    state.items.push(...content.filter((n) => !known.has(n.id)));
                }
                state.page = page;
                state.totalPages = totalPages;
                state.totalElements = totalElements;
                state.listStatus = 'succeeded';
            })
            .addCase(fetchNotes.rejected, (state, action) => {
                state.listStatus = 'failed';
                state.listError = action.payload ?? 'Failed to load notes';
            })
            .addCase(createNote.fulfilled, (state, action) => {
                state.items.unshift(action.payload);
                state.totalElements += 1;
            })
            .addCase(updateNote.fulfilled, (state, action) => {
                const index = state.items.findIndex((n) => n.id === action.payload.id);
                if (index >= 0) {
                    state.items[index] = action.payload;
                }
            })
            .addCase(deleteNote.fulfilled, (state, action) => {
                const before = state.items.length;
                state.items = state.items.filter((n) => n.id !== action.payload);
                state.totalElements = Math.max(0, state.totalElements - (before - state.items.length));
            })
            .addMatcher(isPending(createNote, updateNote, deleteNote), (state) => {
                state.saveStatus = 'loading';
                state.saveError = null;
            })
            .addMatcher(isFulfilled(createNote, updateNote, deleteNote), (state) => {
                state.saveStatus = 'succeeded';
            })
            .addMatcher(isRejected(createNote, updateNote, deleteNote), (state, action) => {
                state.saveStatus = 'failed';
                state.saveError = (action.payload as string | undefined) ?? 'Failed to save note';
            });
    },
});

export const { resetSaveStatus } = notesSlice.actions;
export default notesSlice.reducer;

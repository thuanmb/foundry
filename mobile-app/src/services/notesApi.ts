import api from './api';
import type { Note, NoteInput, PageResponse } from '@/types/note';

export async function listNotes(page = 0, size = 20): Promise<PageResponse<Note>> {
    const { data } = await api.get<PageResponse<Note>>('/notes', { params: { page, size } });
    return data;
}

export async function getNote(id: string): Promise<Note> {
    const { data } = await api.get<Note>(`/notes/${encodeURIComponent(id)}`);
    return data;
}

export async function createNote(input: NoteInput): Promise<Note> {
    const { data } = await api.post<Note>('/notes', input);
    return data;
}

export async function updateNote(id: string, input: NoteInput): Promise<Note> {
    const { data } = await api.put<Note>(`/notes/${encodeURIComponent(id)}`, input);
    return data;
}

export async function deleteNote(id: string): Promise<void> {
    await api.delete(`/notes/${encodeURIComponent(id)}`);
}

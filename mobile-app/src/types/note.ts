export interface Note {
    id: string;
    title: string;
    content: string;
    createdAt: string;
    updatedAt: string;
}

export interface NoteInput {
    title: string;
    content: string;
}

export interface PageResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export const NOTE_TITLE_MAX = 200;
export const NOTE_CONTENT_MAX = 10_000;

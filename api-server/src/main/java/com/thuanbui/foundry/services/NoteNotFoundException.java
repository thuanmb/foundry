package com.thuanbui.foundry.services;

import java.util.UUID;

public class NoteNotFoundException extends RuntimeException {
    public NoteNotFoundException(UUID noteId) {
        super("Note not found: " + noteId);
    }
}

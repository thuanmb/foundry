package com.thuanbui.foundry.services;

import com.thuanbui.foundry.common.model.entity.Note;
import com.thuanbui.foundry.common.repository.NoteRepository;
import com.thuanbui.foundry.model.dto.NoteRequest;
import com.thuanbui.foundry.model.dto.NoteResponse;
import com.thuanbui.foundry.model.dto.PageResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    public static final int MAX_PAGE_SIZE = 100;

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NoteResponse> list(UUID userId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(noteRepository.findByUserId(userId, pageable), NoteResponse::from);
    }

    @Transactional(readOnly = true)
    public NoteResponse get(UUID userId, UUID noteId) {
        return NoteResponse.from(findOwned(userId, noteId));
    }

    @Transactional
    public NoteResponse create(UUID userId, NoteRequest request) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(request.title());
        note.setContent(request.content());
        return NoteResponse.from(noteRepository.save(note));
    }

    @Transactional
    public NoteResponse update(UUID userId, UUID noteId, NoteRequest request) {
        Note note = findOwned(userId, noteId);
        note.setTitle(request.title());
        note.setContent(request.content());
        note.setUpdatedAt(Instant.now());
        return NoteResponse.from(noteRepository.save(note));
    }

    @Transactional
    public void delete(UUID userId, UUID noteId) {
        noteRepository.delete(findOwned(userId, noteId));
    }

    // Other users' notes are reported as not found so their existence is not revealed
    private Note findOwned(UUID userId, UUID noteId) {
        return noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));
    }
}

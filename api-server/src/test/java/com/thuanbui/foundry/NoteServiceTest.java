package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thuanbui.foundry.common.model.entity.Note;
import com.thuanbui.foundry.common.repository.NoteRepository;
import com.thuanbui.foundry.model.dto.NoteRequest;
import com.thuanbui.foundry.model.dto.NoteResponse;
import com.thuanbui.foundry.model.dto.PageResponse;
import com.thuanbui.foundry.services.NoteNotFoundException;
import com.thuanbui.foundry.services.NoteService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @InjectMocks
    private NoteService noteService;

    private UUID userId;
    private UUID otherUserId;
    private Note note;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        note = new Note();
        note.setId(UUID.randomUUID());
        note.setUserId(userId);
        note.setTitle("Title");
        note.setContent("Content");
        note.setCreatedAt(Instant.now());
        note.setUpdatedAt(Instant.now());
    }

    @Test
    void create_shouldPersistNoteForUser() {
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> {
            Note n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        NoteResponse response = noteService.create(userId, new NoteRequest("Hello", "World"));

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        assertEquals(userId, captor.getValue().getUserId());
        assertEquals("Hello", response.title());
        assertEquals("World", response.content());
    }

    @Test
    void list_shouldReturnPageSortedByCreatedAtDesc() {
        when(noteRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(note), inv.getArgument(1), 1));

        PageResponse<NoteResponse> page = noteService.list(userId, 0, 20);

        assertEquals(1, page.content().size());
        assertEquals(note.getId(), page.content().get(0).id());
        assertEquals(0, page.page());
        assertEquals(20, page.size());
        assertEquals(1, page.totalElements());
        assertEquals(1, page.totalPages());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(noteRepository).findByUserId(eq(userId), captor.capture());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), captor.getValue().getSort());
    }

    @Test
    void list_shouldClampPageAndSize() {
        when(noteRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(1), 0));

        noteService.list(userId, -5, 10_000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(noteRepository).findByUserId(eq(userId), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(NoteService.MAX_PAGE_SIZE, captor.getValue().getPageSize());
    }

    @Test
    void get_shouldReturnOwnedNote() {
        when(noteRepository.findByIdAndUserId(note.getId(), userId)).thenReturn(Optional.of(note));

        NoteResponse response = noteService.get(userId, note.getId());

        assertEquals(note.getId(), response.id());
        assertEquals("Title", response.title());
    }

    @Test
    void get_shouldThrowNotFoundForAnotherUsersNote() {
        when(noteRepository.findByIdAndUserId(note.getId(), otherUserId)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteService.get(otherUserId, note.getId()));
    }

    @Test
    void update_shouldModifyOwnedNote() {
        when(noteRepository.findByIdAndUserId(note.getId(), userId)).thenReturn(Optional.of(note));
        when(noteRepository.save(note)).thenReturn(note);

        NoteResponse response = noteService.update(userId, note.getId(), new NoteRequest("New", null));

        assertEquals("New", response.title());
        assertNull(response.content());
        verify(noteRepository).save(note);
    }

    @Test
    void update_shouldThrowNotFoundForAnotherUsersNote() {
        when(noteRepository.findByIdAndUserId(note.getId(), otherUserId)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class,
                () -> noteService.update(otherUserId, note.getId(), new NoteRequest("Hijack", "x")));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void delete_shouldRemoveOwnedNote() {
        when(noteRepository.findByIdAndUserId(note.getId(), userId)).thenReturn(Optional.of(note));

        noteService.delete(userId, note.getId());

        verify(noteRepository).delete(note);
    }

    @Test
    void delete_shouldThrowNotFoundForAnotherUsersNote() {
        when(noteRepository.findByIdAndUserId(note.getId(), otherUserId)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteService.delete(otherUserId, note.getId()));
        verify(noteRepository, never()).delete(any(Note.class));
    }
}

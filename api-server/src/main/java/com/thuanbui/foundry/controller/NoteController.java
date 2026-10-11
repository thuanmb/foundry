package com.thuanbui.foundry.controller;

import com.thuanbui.foundry.model.dto.NoteRequest;
import com.thuanbui.foundry.model.dto.NoteResponse;
import com.thuanbui.foundry.model.dto.PageResponse;
import com.thuanbui.foundry.security.FeatureFlag;
import com.thuanbui.foundry.security.RequestIdentity;
import com.thuanbui.foundry.security.UnauthorizedException;
import com.thuanbui.foundry.services.NoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController extends BaseApiController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    @FeatureFlag("notes")
    public PageResponse<NoteResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return noteService.list(requireUserId(request), page, size);
    }

    @PostMapping
    @FeatureFlag("notes")
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody NoteRequest body, HttpServletRequest request) {
        NoteResponse created = noteService.create(requireUserId(request), body);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @FeatureFlag("notes")
    public NoteResponse get(@PathVariable UUID id, HttpServletRequest request) {
        return noteService.get(requireUserId(request), id);
    }

    @PutMapping("/{id}")
    @FeatureFlag("notes")
    public NoteResponse update(@PathVariable UUID id, @Valid @RequestBody NoteRequest body,
            HttpServletRequest request) {
        return noteService.update(requireUserId(request), id, body);
    }

    @DeleteMapping("/{id}")
    @FeatureFlag("notes")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
        noteService.delete(requireUserId(request), id);
        return ResponseEntity.noContent().build();
    }

    private static UUID requireUserId(HttpServletRequest request) {
        UUID userId = RequestIdentity.userId(request);
        if (userId == null) {
            throw new UnauthorizedException();
        }
        return userId;
    }
}

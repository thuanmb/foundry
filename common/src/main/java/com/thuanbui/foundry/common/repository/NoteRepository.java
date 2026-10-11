package com.thuanbui.foundry.common.repository;

import com.thuanbui.foundry.common.model.entity.Note;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, UUID> {

    Page<Note> findByUserId(UUID userId, Pageable pageable);

    Optional<Note> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("DELETE FROM Note n WHERE n.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}

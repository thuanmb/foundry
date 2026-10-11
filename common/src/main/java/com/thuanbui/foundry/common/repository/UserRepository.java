package com.thuanbui.foundry.common.repository;

import com.thuanbui.foundry.common.model.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByDeletedTrueAndDeletionScheduledAtBefore(Instant before);
}

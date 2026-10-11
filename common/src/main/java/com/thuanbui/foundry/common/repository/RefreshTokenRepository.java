package com.thuanbui.foundry.common.repository;

import com.thuanbui.foundry.common.model.entity.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    long countByUserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Query(value = """
            UPDATE refresh_tokens
            SET revoked = TRUE, revoked_at = CURRENT_TIMESTAMP
            WHERE user_id = :userId AND revoked = FALSE
            """, nativeQuery = true)
    int revokeAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query(value = """
            DELETE FROM refresh_tokens
            WHERE user_id = :userId AND revoked = FALSE
              AND id NOT IN (
                SELECT id FROM refresh_tokens
                WHERE user_id = :userId AND revoked = FALSE
                ORDER BY created_at DESC
                LIMIT :keepCount
              )
            """, nativeQuery = true)
    int deleteOldestActiveByUserId(@Param("userId") UUID userId, @Param("keepCount") int keepCount);
}

package com.thuanbui.foundry.common.repository;

import com.thuanbui.foundry.common.model.entity.OtpVerification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    @Query(value = """
            SELECT * FROM otp_verifications
            WHERE phone_number = :phoneNumber
              AND verified = FALSE
              AND expires_at > NOW()
            ORDER BY created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<OtpVerification> findLatestActiveByPhone(@Param("phoneNumber") String phoneNumber);

    @Query(value = """
            SELECT COUNT(*) FROM otp_verifications
            WHERE phone_number = :phoneNumber
              AND created_at > :since
            """, nativeQuery = true)
    long countRecentByPhone(@Param("phoneNumber") String phoneNumber, @Param("since") Instant since);

    @Query(value = """
            SELECT * FROM otp_verifications
            WHERE phone_number = :phoneNumber
              AND verified = FALSE
              AND expires_at > NOW()
            ORDER BY created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<OtpVerification> findLatestByPhoneAndNotExpired(@Param("phoneNumber") String phoneNumber);

    @Modifying
    @Query(value = """
            UPDATE otp_verifications
            SET verified = TRUE
            WHERE phone_number = :phoneNumber
              AND verified = FALSE
              AND expires_at > NOW()
            """, nativeQuery = true)
    int expirePreviousOtps(@Param("phoneNumber") String phoneNumber);
}

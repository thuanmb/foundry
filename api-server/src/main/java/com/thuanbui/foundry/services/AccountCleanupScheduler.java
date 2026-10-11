package com.thuanbui.foundry.services;

import com.thuanbui.foundry.common.model.entity.User;
import com.thuanbui.foundry.common.repository.NoteRepository;
import com.thuanbui.foundry.common.repository.UserRepository;
import com.thuanbui.foundry.config.AccountProperties;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccountCleanupScheduler.class);

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final AccountProperties accountProperties;

    public AccountCleanupScheduler(UserRepository userRepository, NoteRepository noteRepository,
            AccountProperties accountProperties) {
        this.userRepository = userRepository;
        this.noteRepository = noteRepository;
        this.accountProperties = accountProperties;
    }

    @Scheduled(cron = "${account.deletion.cleanup-cron:0 0 3 * * ?}")
    @Transactional
    public void purgeExpiredDeletions() {
        Instant now = Instant.now();
        List<User> expiredUsers = userRepository.findByDeletedTrueAndDeletionScheduledAtBefore(now);

        if (expiredUsers.isEmpty()) {
            return;
        }

        for (User user : expiredUsers) {
            noteRepository.deleteByUserId(user.getId());
        }

        if (accountProperties.deletion().anonymizeOnDelete()) {
            for (User user : expiredUsers) {
                user.setPhoneNumber(anonymizedPhone(user));
                user.setDisplayName(null);
                user.setDeletionScheduledAt(null);
                user.setUpdatedAt(now);
            }
            userRepository.saveAll(expiredUsers);
        }

        log.info("Purged {} expired account deletions", expiredUsers.size());
    }

    // phone_number is VARCHAR(20) and unique: "del_" + 16 hex chars of the UUID's random bits
    static String anonymizedPhone(User user) {
        return String.format("del_%016x", user.getId().getLeastSignificantBits());
    }
}

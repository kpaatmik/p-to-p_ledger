package com.paymentledger.service;

import com.paymentledger.entity.User;
import com.paymentledger.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Handles automatic account unlocking.
 * Called by the {@code AccountUnlockJob} scheduler every 5 minutes.
 */
@Service
public class AccountLockService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AccountLockService(UserRepository userRepository,
                               NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Finds every locked account whose {@code lock_expires_at} is in the past,
     * unlocks it, resets the failed login counter, and notifies the user.
     *
     * <p>Safe to call repeatedly — accounts already unlocked are not touched.
     *
     * @return the number of accounts unlocked in this run
     */
    @Transactional
    public int unlockEligibleAccounts() {
        Instant now = Instant.now();

        // Load all locked users whose lock period has passed
        List<User> eligible = userRepository.findAll().stream()
                .filter(u -> u.isAccountLocked()
                        && u.getLockExpiresAt() != null
                        && u.getLockExpiresAt().isBefore(now))
                .toList();

        for (User user : eligible) {
            user.setAccountLocked(false);
            user.setLockExpiresAt(null);
            user.setFailedLoginCount(0);
            userRepository.save(user);

            notificationService.send(
                    user,
                    NotificationService.TYPE_ACCOUNT_UNLOCKED,
                    "Your account has been automatically unlocked. You may now sign in.");
        }

        return eligible.size();
    }
}

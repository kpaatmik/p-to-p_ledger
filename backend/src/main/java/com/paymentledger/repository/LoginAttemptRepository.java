package com.paymentledger.repository;

import com.paymentledger.entity.LoginAttempt;
import com.paymentledger.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    /**
     * Counts consecutive failed login attempts for a user after a given timestamp.
     * Used to determine whether an account should be locked.
     */
    long countByUserAndSuccessIsFalseAndAttemptedAtAfter(User user, Instant since);

    /**
     * Returns the total number of failed attempts for a user since a given time,
     * regardless of whether any successes occurred in between.
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la " +
           "WHERE la.user = :user AND la.success = false AND la.attemptedAt > :since")
    long countFailedSince(@Param("user") User user, @Param("since") Instant since);
}

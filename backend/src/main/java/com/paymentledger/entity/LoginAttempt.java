package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Records every login attempt (success or failure) for a user.
 * Maps to the {@code login_attempts} table.
 */
@Entity
@Table(
    name = "login_attempts",
    indexes = {
        @Index(name = "idx_login_attempts_user_id", columnList = "user_id"),
        @Index(name = "idx_login_attempts_attempted_at", columnList = "attempted_at")
    }
)
public class LoginAttempt {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    @Column(name = "success", nullable = false)
    private boolean success;

    /** IPv4 or IPv6 address of the client. May be null if not available. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // ── Constructors ───────────────────────────────────────────────────────

    protected LoginAttempt() {}

    public LoginAttempt(User user, boolean success, String ipAddress) {
        this.user = user;
        this.success = success;
        this.ipAddress = ipAddress;
        this.attemptedAt = Instant.now();
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Instant getAttemptedAt() { return attemptedAt; }
    public boolean isSuccess() { return success; }
    public String getIpAddress() { return ipAddress; }
}

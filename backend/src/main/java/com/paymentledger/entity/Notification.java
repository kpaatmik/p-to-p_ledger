package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * In-app notification for a user.
 * Maps to the {@code notifications} table.
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_user_id", columnList = "user_id"),
        @Index(name = "idx_notifications_read",    columnList = "read")
    }
)
public class Notification {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Notification category, e.g. TRANSFER_INITIATED, SETTLEMENT_SUCCESS, ACCOUNT_LOCKED. */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // ── Constructors ───────────────────────────────────────────────────────

    protected Notification() {}

    public Notification(User user, String type, String message) {
        this.user = user;
        this.type = type;
        this.message = message;
    }

    // ── Getters and setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Instant getCreatedAt() { return createdAt; }
}

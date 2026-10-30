package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * A registered merchant eligible to receive Check Please payments.
 * Maps to the {@code merchants} table.
 */
@Entity
@Table(
    name = "merchants",
    indexes = {
        @Index(name = "idx_merchants_email",  columnList = "email"),
        @Index(name = "idx_merchants_status", columnList = "status")
    }
)
public class Merchant {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MerchantStatus status = MerchantStatus.ACTIVE;

    /** Admin user who registered this merchant. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ── Constructors ───────────────────────────────────────────────────────

    protected Merchant() {}

    public Merchant(String name, String email, User registeredBy) {
        this.name = name;
        this.email = email;
        this.registeredBy = registeredBy;
    }

    // ── Getters and setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public MerchantStatus getStatus() { return status; }
    public void setStatus(MerchantStatus status) { this.status = status; }
    public User getRegisteredBy() { return registeredBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores a user's wallet balances and status.
 * Maps to the {@code wallets} table.
 *
 * <p>Financial invariants enforced at the database level:
 * <ul>
 *   <li>{@code total_balance >= 0}</li>
 *   <li>{@code reserved_balance >= 0}</li>
 *   <li>{@code total_balance >= reserved_balance}</li>
 * </ul>
 *
 * <p>{@code available_balance} is NOT stored — compute as
 * {@code total_balance - reserved_balance} in the service layer.
 */
@Entity
@Table(
    name = "wallets",
    indexes = {
        @Index(name = "idx_wallets_user_id", columnList = "user_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_wallets_user_id", columnNames = "user_id")
    }
)
@Check(constraints = "total_balance >= 0 AND reserved_balance >= 0 AND total_balance >= reserved_balance")
public class Wallet {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** One wallet per user — enforced by unique constraint on user_id. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalBalance = BigDecimal.ZERO;

    @Column(name = "reserved_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletStatus status = WalletStatus.ACTIVE;

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

    protected Wallet() {}

    public Wallet(User user) {
        this.user = user;
    }

    // ── Getters and setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public BigDecimal getTotalBalance() { return totalBalance; }
    public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }
    public BigDecimal getReservedBalance() { return reservedBalance; }
    public void setReservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; }
    public WalletStatus getStatus() { return status; }
    public void setStatus(WalletStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

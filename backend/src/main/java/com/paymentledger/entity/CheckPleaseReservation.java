package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A user's declared payment intent for a specific merchant, with a maximum
 * reserved amount and user-selected expiry.
 * Maps to the {@code check_please_reservations} table.
 *
 * <p>Database CHECK constraints:
 * <ul>
 *   <li>{@code max_amount > 0}</li>
 *   <li>{@code remaining_amount >= 0}</li>
 *   <li>{@code remaining_amount <= max_amount}</li>
 * </ul>
 *
 * <p>Creating a reservation increases {@code wallet.reserved_balance} by
 * {@code max_amount} without changing {@code total_balance}.
 * After a successful payment, {@code total_balance} decreases by the payment
 * amount and {@code reserved_balance} decreases by the full remaining amount.
 * Cancellation or expiry decreases only {@code reserved_balance}.
 */
@Entity
@Table(
    name = "check_please_reservations",
    indexes = {
        @Index(name = "idx_reservations_user_id",     columnList = "user_id"),
        @Index(name = "idx_reservations_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_reservations_status",      columnList = "status"),
        @Index(name = "idx_reservations_expires_at",  columnList = "expires_at")
    }
)
@Check(constraints = "max_amount > 0 AND remaining_amount >= 0 AND remaining_amount <= max_amount")
public class CheckPleaseReservation {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false, updatable = false)
    private Merchant merchant;

    @Column(name = "max_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal maxAmount;

    @Column(name = "remaining_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

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

    protected CheckPleaseReservation() {}

    public CheckPleaseReservation(User user, Wallet wallet, Merchant merchant,
                                  BigDecimal maxAmount, Instant expiresAt) {
        this.user = user;
        this.wallet = wallet;
        this.merchant = merchant;
        this.maxAmount = maxAmount;
        this.remainingAmount = maxAmount;
        this.expiresAt = expiresAt;
    }

    // ── Getters and setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Wallet getWallet() { return wallet; }
    public Merchant getMerchant() { return merchant; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

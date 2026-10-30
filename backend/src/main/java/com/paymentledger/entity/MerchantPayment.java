package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A merchant payment request against an active Check Please reservation.
 * Maps to the {@code merchant_payments} table.
 *
 * <p>Idempotency is enforced by the UNIQUE constraint on {@code request_id}.
 * Merchant payments are captured immediately — there is no delayed settlement.
 */
@Entity
@Table(
    name = "merchant_payments",
    indexes = {
        @Index(name = "idx_merchant_payments_request_id",     columnList = "request_id"),
        @Index(name = "idx_merchant_payments_reservation_id", columnList = "reservation_id"),
        @Index(name = "idx_merchant_payments_user_id",        columnList = "user_id")
    }
)
public class MerchantPayment {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Client-generated idempotency key. UNIQUE constraint prevents duplicate processing. */
    @Column(name = "request_id", nullable = false, unique = true, length = 255)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private CheckPleaseReservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    /**
     * The ledger entry created when the payment is captured.
     * Null until capture is completed successfully.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_entry_id")
    private LedgerEntry ledgerEntry;

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

    protected MerchantPayment() {}

    public MerchantPayment(String requestId, CheckPleaseReservation reservation,
                           User user, Merchant merchant, Wallet wallet,
                           BigDecimal amount) {
        this.requestId = requestId;
        this.reservation = reservation;
        this.user = user;
        this.merchant = merchant;
        this.wallet = wallet;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    // ── Getters and setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getRequestId() { return requestId; }
    public CheckPleaseReservation getReservation() { return reservation; }
    public User getUser() { return user; }
    public Merchant getMerchant() { return merchant; }
    public Wallet getWallet() { return wallet; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public LedgerEntry getLedgerEntry() { return ledgerEntry; }
    public void setLedgerEntry(LedgerEntry ledgerEntry) { this.ledgerEntry = ledgerEntry; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

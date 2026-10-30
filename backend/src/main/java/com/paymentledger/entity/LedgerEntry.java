package com.paymentledger.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * An immutable financial debit or credit record.
 * Maps to the {@code ledger_entries} table.
 *
 * <p>Each P2P transfer creates a DEBIT entry for the sender at initiation
 * and a CREDIT entry for the receiver at settlement. Ledger entries are
 * never updated after creation.
 */
@Entity
@Table(
    name = "ledger_entries",
    indexes = {
        @Index(name = "idx_ledger_entries_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_ledger_entries_wallet_id",      columnList = "wallet_id")
    }
)
public class LedgerEntry {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "transaction_id", nullable = true, updatable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10, updatable = false)
    private EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(name = "description", length = 500, updatable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // ── Constructors ───────────────────────────────────────────────────────

    protected LedgerEntry() {}

    public LedgerEntry(Transaction transaction, Wallet wallet,
                       EntryType entryType, BigDecimal amount, String description) {
        this.transaction = transaction;
        this.wallet = wallet;
        this.entryType = entryType;
        this.amount = amount;
        this.description = description;
    }

    // ── Getters (no setters — ledger entries are immutable) ────────────────

    public UUID getId() { return id; }
    public Transaction getTransaction() { return transaction; }
    public Wallet getWallet() { return wallet; }
    public EntryType getEntryType() { return entryType; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}

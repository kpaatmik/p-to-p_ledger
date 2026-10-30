package com.paymentledger.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.paymentledger.entity.EntryType;
import com.paymentledger.entity.LedgerEntry;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionIdOrderByCreatedAtAsc(UUID transactionId);

    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    // ── Today's spending for wallet balance display ────────────────────────

    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le " +
           "WHERE le.wallet.id = :walletId AND le.entryType = :type " +
           "AND le.createdAt >= :since")
    BigDecimal sumByWalletAndTypeSince(@Param("walletId") UUID walletId,
                                       @Param("type") EntryType type,
                                       @Param("since") Instant since);

    // ── Reconciliation: total settled debits / credits ─────────────────────

    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le " +
           "WHERE le.entryType = :entryType " +
           "AND le.transaction.status = com.paymentledger.entity.TransactionStatus.SUCCESS " +
           "AND le.transaction.type = com.paymentledger.entity.TransactionType.P2P_TRANSFER")
    BigDecimal sumSettledByEntryType(@Param("entryType") EntryType entryType);

    /**
     * Sum of all settled DEBIT ledger entries (money sent out) for P2P transactions on the given date.
     * Used for daily reconciliation.
     */
    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le " +
           "WHERE le.entryType = com.paymentledger.entity.EntryType.DEBIT " +
           "AND le.transaction.status = com.paymentledger.entity.TransactionStatus.SUCCESS " +
           "AND le.transaction.type = com.paymentledger.entity.TransactionType.P2P_TRANSFER " +
           "AND CAST(le.transaction.settledAt AS date) = :date")
    BigDecimal sumSettledDebits(@Param("date") LocalDate date);

    /**
     * Sum of all settled CREDIT ledger entries (money received) for P2P transactions on the given date.
     * Used for daily reconciliation.
     */
    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le " +
           "WHERE le.entryType = com.paymentledger.entity.EntryType.CREDIT " +
           "AND le.transaction.status = com.paymentledger.entity.TransactionStatus.SUCCESS " +
           "AND le.transaction.type = com.paymentledger.entity.TransactionType.P2P_TRANSFER " +
           "AND CAST(le.transaction.settledAt AS date) = :date")
    BigDecimal sumSettledCredits(@Param("date") LocalDate date);
}

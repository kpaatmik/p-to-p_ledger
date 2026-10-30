package com.paymentledger.repository;

import com.paymentledger.entity.EntryType;
import com.paymentledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
}

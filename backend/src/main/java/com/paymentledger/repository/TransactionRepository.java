package com.paymentledger.repository;

import com.paymentledger.entity.Transaction;
import com.paymentledger.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /** Idempotency lookup — check before processing. */
    Optional<Transaction> findByRequestId(String requestId);

    boolean existsByRequestId(String requestId);

    // ── Paginated history for a user (sent + received) ─────────────────────

    @Query("SELECT t FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId) " +
           "AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdAndStatus(@Param("userId") UUID userId,
                                            @Param("status") TransactionStatus status,
                                            Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId) " +
           "AND t.createdAt BETWEEN :from AND :to ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                               @Param("from") Instant from,
                                               @Param("to") Instant to,
                                               Pageable pageable);

    // ── Mini-statement (last 10) ───────────────────────────────────────────

    @Query("SELECT t FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId) " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findTop10ByUserId(@Param("userId") UUID userId, Pageable pageable);

    // ── Sender-only (sent transfers) ───────────────────────────────────────

    Page<Transaction> findBySenderIdOrderByCreatedAtDesc(UUID senderId, Pageable pageable);

    // ── Receiver-only (received transfers) ────────────────────────────────

    Page<Transaction> findByReceiverIdOrderByCreatedAtDesc(UUID receiverId, Pageable pageable);

    // ── Settlement job queries ─────────────────────────────────────────────

    /** All PENDING transfers whose settlement date is today or earlier. */
    List<Transaction> findByStatusAndSettlementDueDateLessThanEqual(
            TransactionStatus status, LocalDate dueDate);

    // ── Daily limit fraud check ────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.sender.id = :senderId " +
           "AND t.status NOT IN (com.paymentledger.entity.TransactionStatus.FAILED, " +
           "com.paymentledger.entity.TransactionStatus.DECLINED) " +
           "AND t.createdAt >= :startOfDay")
    BigDecimal sumSentAmountSince(@Param("senderId") UUID senderId,
                                  @Param("startOfDay") Instant startOfDay);

    // ── Duplicate detection fraud check ───────────────────────────────────

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.sender.id = :senderId AND t.receiver.id = :receiverId " +
           "AND t.amount = :amount AND t.createdAt >= :since " +
           "AND t.status IN (com.paymentledger.entity.TransactionStatus.PENDING, " +
           "com.paymentledger.entity.TransactionStatus.SUCCESS)")
    long countRecentDuplicates(@Param("senderId") UUID senderId,
                                @Param("receiverId") UUID receiverId,
                                @Param("amount") BigDecimal amount,
                                @Param("since") Instant since);

    // ── Admin monitoring ───────────────────────────────────────────────────

    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status, Pageable pageable);

    // ── Reconciliation ─────────────────────────────────────────────────────

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status " +
           "AND t.settlementDueDate <= :today")
    long countOverdue(@Param("status") TransactionStatus status, @Param("today") LocalDate today);
}

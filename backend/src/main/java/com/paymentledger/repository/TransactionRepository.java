package com.paymentledger.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.paymentledger.entity.Transaction;
import com.paymentledger.entity.TransactionStatus;

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

    /** All PENDING transfers whose settlement date is today or earlier (paginated). */
    Page<Transaction> findByStatusAndSettlementDueDateLessThanEqual(
            TransactionStatus status, LocalDate dueDate, Pageable pageable);

    /** Legacy non-paginated version for backward compatibility. */
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

    @Query("SELECT COALESCE(COUNT(t), 0) FROM Transaction t WHERE t.status = :status " +
           "AND t.settlementDueDate <= :today")
    long countOverdue(@Param("status") TransactionStatus status, @Param("today") LocalDate today);

    // ── T-053: Transaction history with filters ──────────────────────────

    @Query("SELECT t FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId) " +
           "AND t.status = :status AND t.createdAt BETWEEN :from AND :to " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdStatusAndDateRange(@Param("userId") UUID userId,
                                                      @Param("status") TransactionStatus status,
                                                      @Param("from") Instant from,
                                                      @Param("to") Instant to,
                                                      Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sender.id = :userId " +
           "AND t.status = :status AND t.createdAt BETWEEN :from AND :to " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findSentByStatusAndDateRange(@Param("userId") UUID userId,
                                                    @Param("status") TransactionStatus status,
                                                    @Param("from") Instant from,
                                                    @Param("to") Instant to,
                                                    Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.receiver.id = :userId " +
           "AND t.status = :status AND t.createdAt BETWEEN :from AND :to " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findReceivedByStatusAndDateRange(@Param("userId") UUID userId,
                                                        @Param("status") TransactionStatus status,
                                                        @Param("from") Instant from,
                                                        @Param("to") Instant to,
                                                        Pageable pageable);

    // ── Daily summary metrics (T-096) ──────────────────────────────────────

    /**
     * Count of PENDING or SUCCESS P2P transfers created on the given date (IST).
     * Used for daily initiated transaction metric.
     */
    @Query("SELECT COALESCE(COUNT(t), 0) FROM Transaction t " +
           "WHERE t.type = com.paymentledger.entity.TransactionType.P2P_TRANSFER " +
           "AND t.status IN (com.paymentledger.entity.TransactionStatus.PENDING, " +
           "com.paymentledger.entity.TransactionStatus.SUCCESS) " +
           "AND CAST(t.createdAt AS date) = :date")
    long countInitiatedToday(@Param("date") LocalDate date);

    /**
     * Sum of amounts for PENDING or SUCCESS P2P transfers created on the given date.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = com.paymentledger.entity.TransactionType.P2P_TRANSFER " +
           "AND t.status IN (com.paymentledger.entity.TransactionStatus.PENDING, " +
           "com.paymentledger.entity.TransactionStatus.SUCCESS) " +
           "AND CAST(t.createdAt AS date) = :date")
    BigDecimal sumInitiatedAmountToday(@Param("date") LocalDate date);

    /**
     * Count of SUCCESS P2P transfers settled on the given date.
     */
    @Query("SELECT COALESCE(COUNT(t), 0) FROM Transaction t " +
           "WHERE t.type = com.paymentledger.entity.TransactionType.P2P_TRANSFER " +
           "AND t.status = com.paymentledger.entity.TransactionStatus.SUCCESS " +
           "AND CAST(t.settledAt AS date) = :date")
    long countSettledToday(@Param("date") LocalDate date);

    /**
     * Sum of amounts for SUCCESS P2P transfers settled on the given date.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = com.paymentledger.entity.TransactionType.P2P_TRANSFER " +
           "AND t.status = com.paymentledger.entity.TransactionStatus.SUCCESS " +
           "AND CAST(t.settledAt AS date) = :date")
    BigDecimal sumSettledAmountToday(@Param("date") LocalDate date);

    /**
     * Sum of amounts for all PENDING P2P transfers (awaiting settlement).
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = com.paymentledger.entity.TransactionType.P2P_TRANSFER " +
           "AND t.status = com.paymentledger.entity.TransactionStatus.PENDING")
    BigDecimal sumPendingSettlementAmount();
}

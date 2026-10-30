package com.paymentledger.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.paymentledger.entity.AlertStatus;
import com.paymentledger.entity.FraudAlert;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    /** Admin fraud review queue — all PENDING alerts. */
    List<FraudAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    Page<FraudAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status, Pageable pageable);

    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(AlertStatus status);

    List<FraudAlert> findByTransactionId(UUID transactionId);

    // ── Daily summary metrics (T-096) ──────────────────────────────────────

    /**
     * Count of fraud alerts created on the given date (IST).
     */
    @Query("SELECT COALESCE(COUNT(fa), 0) FROM FraudAlert fa " +
           "WHERE CAST(fa.createdAt AS date) = :date")
    long countCreatedToday(@Param("date") LocalDate date);
}


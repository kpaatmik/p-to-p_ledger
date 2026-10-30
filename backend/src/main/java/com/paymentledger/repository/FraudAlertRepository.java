package com.paymentledger.repository;

import com.paymentledger.entity.AlertStatus;
import com.paymentledger.entity.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    /** Admin fraud review queue — all PENDING alerts. */
    List<FraudAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    Page<FraudAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status, Pageable pageable);

    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(AlertStatus status);

    List<FraudAlert> findByTransactionId(UUID transactionId);
}

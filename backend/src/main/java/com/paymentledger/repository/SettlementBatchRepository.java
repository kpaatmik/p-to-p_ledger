package com.paymentledger.repository;

import com.paymentledger.entity.SettlementBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, UUID> {

    List<SettlementBatch> findByJobLogIdOrderByBatchRunAtDesc(UUID jobLogId);
}

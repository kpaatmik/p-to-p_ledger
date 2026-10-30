package com.paymentledger.repository;

import com.paymentledger.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailySummaryRepository extends JpaRepository<DailySummary, UUID> {

    Optional<DailySummary> findBySummaryDate(LocalDate summaryDate);
}

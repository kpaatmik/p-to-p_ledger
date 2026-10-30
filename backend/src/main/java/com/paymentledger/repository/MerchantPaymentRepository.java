package com.paymentledger.repository;

import com.paymentledger.entity.MerchantPayment;
import com.paymentledger.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantPaymentRepository extends JpaRepository<MerchantPayment, UUID> {

    /** Idempotency lookup — check before processing. */
    Optional<MerchantPayment> findByRequestId(String requestId);

    boolean existsByRequestId(String requestId);

    List<MerchantPayment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<MerchantPayment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<MerchantPayment> findByReservationIdOrderByCreatedAtDesc(UUID reservationId);

    List<MerchantPayment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
}

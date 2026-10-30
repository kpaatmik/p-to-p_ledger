package com.paymentledger.repository;

import com.paymentledger.entity.CheckPleaseReservation;
import com.paymentledger.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CheckPleaseReservationRepository extends JpaRepository<CheckPleaseReservation, UUID> {

    /** All reservations for a user (all statuses). */
    Page<CheckPleaseReservation> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Reservations for a user filtered by status. */
    List<CheckPleaseReservation> findByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId, ReservationStatus status);

    /**
     * All ACTIVE reservations whose expiry has passed.
     * Used by the reservation expiry processor job.
     */
    @Query("SELECT r FROM CheckPleaseReservation r " +
           "WHERE r.status = com.paymentledger.entity.ReservationStatus.ACTIVE " +
           "AND r.expiresAt <= :now")
    List<CheckPleaseReservation> findExpiredActive(@Param("now") Instant now);

    /**
     * Acquires a pessimistic write lock on a reservation row.
     * Use this before consuming, cancelling, or expiring a reservation
     * to prevent concurrent updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CheckPleaseReservation r WHERE r.id = :id")
    Optional<CheckPleaseReservation> findByIdWithLock(@Param("id") UUID id);
}

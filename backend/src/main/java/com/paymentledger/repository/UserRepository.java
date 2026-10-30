package com.paymentledger.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.paymentledger.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // ── Daily summary metrics (T-096) ──────────────────────────────────────

    /**
     * Count of users created on the given date (IST).
     */
    @Query("SELECT COALESCE(COUNT(u), 0) FROM User u " +
           "WHERE CAST(u.createdAt AS date) = :date")
    long countCreatedToday(@Param("date") LocalDate date);

    /**
     * Find users with a specific role (e.g., "ADMIN").
     */
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") String role);
}


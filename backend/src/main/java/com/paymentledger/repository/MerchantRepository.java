package com.paymentledger.repository;

import com.paymentledger.entity.Merchant;
import com.paymentledger.entity.MerchantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByEmail(String email);

    boolean existsByEmail(String email);

    /** User-facing merchant list — returns only ACTIVE merchants. */
    List<Merchant> findByStatusOrderByNameAsc(MerchantStatus status);

    /** Case-insensitive name search for user-facing merchant list. */
    List<Merchant> findByStatusAndNameContainingIgnoreCaseOrderByNameAsc(
            MerchantStatus status, String name);

    Page<Merchant> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

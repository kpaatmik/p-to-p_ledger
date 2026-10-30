package com.paymentledger.repository;

import com.paymentledger.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Paginated notification history for a user, newest first. */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Count of unread notifications — displayed on the notification bell. */
    long countByUserIdAndReadIsFalse(UUID userId);
}

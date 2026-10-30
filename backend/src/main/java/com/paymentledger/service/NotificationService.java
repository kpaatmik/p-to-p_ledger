package com.paymentledger.service;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.dto.response.NotificationResponse;
import com.paymentledger.entity.Notification;
import com.paymentledger.entity.User;
import com.paymentledger.repository.NotificationRepository;
import com.paymentledger.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Creates in-app notifications.
 * All other services call this to persist notifications without knowing
 * about the Notification entity directly.
 *
 * <p>Uses {@code REQUIRES_NEW} so a notification failure never rolls back
 * the calling financial transaction.
 */
@Service
public class NotificationService {

    // Notification type constants — shared across all services
    public static final String TYPE_ACCOUNT_LOCKED   = "ACCOUNT_LOCKED";
    public static final String TYPE_ACCOUNT_UNLOCKED = "ACCOUNT_UNLOCKED";
    public static final String TYPE_TRANSFER_INITIATED   = "TRANSFER_INITIATED";
    public static final String TYPE_SETTLEMENT_SUCCESS   = "SETTLEMENT_SUCCESS";
    public static final String TYPE_SETTLEMENT_FAILED    = "SETTLEMENT_FAILED";
    public static final String TYPE_SETTLEMENT_DECLINED  = "SETTLEMENT_DECLINED";
    public static final String TYPE_FRAUD_ALERT          = "FRAUD_ALERT";
    public static final String TYPE_TRANSACTION_BLOCKED  = "TRANSACTION_BLOCKED";
    public static final String TYPE_RESERVATION_CREATED  = "RESERVATION_CREATED";
    public static final String TYPE_RESERVATION_CANCELLED = "RESERVATION_CANCELLED";
    public static final String TYPE_RESERVATION_EXPIRED  = "RESERVATION_EXPIRED";
    public static final String TYPE_MERCHANT_PAYMENT_SUCCESS = "MERCHANT_PAYMENT_SUCCESS";
    public static final String TYPE_MERCHANT_PAYMENT_FAILED  = "MERCHANT_PAYMENT_FAILED";
    public static final String TYPE_RECONCILIATION_MISMATCH  = "RECONCILIATION_MISMATCH";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Persists a new unread notification for {@code user}.
     * Runs in its own transaction so notification persistence does not
     * interfere with the caller's transaction boundary.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(User user, String type, String message) {
        createNotification(user.getId(), type, message);
    }

    /**
     * T-100: Centralized notification creation method.
     * All services should call this method instead of directly creating notifications.
     * 
     * Runs in its own transaction (REQUIRES_NEW) so notification persistence
     * does not interfere with the caller's transaction boundary.
     * 
     * @param userId the user ID to receive the notification
     * @param type the notification type (use TYPE_* constants)
     * @param message the notification message
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(UUID userId, String type, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        notificationRepository.save(new Notification(user, type, message));
    }

    /**
     * T-101: Get paginated notifications for a user, ordered by createdAt DESC.
     * 
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of notification responses
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        var responses = notifications.getContent().stream()
                .map(n -> new NotificationResponse(
                        n.getId(),
                        n.getType(),
                        n.getMessage(),
                        n.isRead(),
                        n.getCreatedAt()
                ))
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, notifications.getTotalElements());
    }

    /**
     * T-101: Mark a notification as read.
     * 
     * Verifies ownership: the authenticated user must be the notification's owner.
     * Idempotent: marking an already-read notification succeeds silently.
     * 
     * @param notificationId the notification ID
     * @param userId the authenticated user ID (owner verification)
     * @throws EntityNotFoundException if notification not found or not owned by user
     */
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationId));

        // Ownership check
        if (!notification.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Notification not found: " + notificationId);
        }

        // Idempotent: mark as read (no-op if already read)
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * T-102: Get unread notification count for a user.
     * 
     * Uses a database count query rather than loading notification records.
     * 
     * @param userId the user ID
     * @return count of unread notifications
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadIsFalse(userId);
    }
}



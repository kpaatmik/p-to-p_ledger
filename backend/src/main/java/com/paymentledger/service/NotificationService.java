package com.paymentledger.service;

import com.paymentledger.entity.Notification;
import com.paymentledger.entity.User;
import com.paymentledger.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Persists a new unread notification for {@code user}.
     * Runs in its own transaction so notification persistence does not
     * interfere with the caller's transaction boundary.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(User user, String type, String message) {
        notificationRepository.save(new Notification(user, type, message));
    }
}

package com.paymentledger.entity;

/**
 * Enumeration of notification types.
 * Used to categorize notifications created during various system events.
 */
public enum NotificationType {
    TRANSFER_INITIATED,      // P2P transfer started
    TRANSFER_SETTLED,        // P2P transfer settlement completed
    SETTLEMENT_SUCCESS,      // Batch settlement succeeded
    ACCOUNT_LOCKED,          // User account locked after failed logins
    FRAUD_ALERT,            // Fraud detection alert
    WALLET_FROZEN,          // Admin froze wallet
    WALLET_UNFROZEN         // Admin unfroze wallet
}

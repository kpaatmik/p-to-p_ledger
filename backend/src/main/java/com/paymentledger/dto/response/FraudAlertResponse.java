package com.paymentledger.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response for fraud alert details (T-063).
 * Used in admin fraud alert queue and detail views.
 * 
 * For approved transaction alerts (HIGH_VALUE):
 * - amount, senderEmail, recipientEmail from the associated transaction
 * 
 * For rejected attempt alerts (DAILY_LIMIT, DUPLICATE):
 * - amount, senderEmail, recipientEmail from the attempt metadata
 */
public record FraudAlertResponse(
    UUID alertId,
    UUID transactionId,           // null for rejected-attempt alerts
    String alertType,              // DAILY_LIMIT, DUPLICATE, HIGH_VALUE
    String status,                 // PENDING, BLOCKED, DISMISSED
    BigDecimal amount,
    String senderEmail,
    String recipientEmail,
    Instant createdAt,
    Instant reviewedAt,            // null if not yet reviewed
    String reviewedByEmail,        // null if not yet reviewed
    String reviewedAction          // null if not yet reviewed; "BLOCKED" or "DISMISSED"
) {}

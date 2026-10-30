package com.paymentledger.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response for transaction list item (T-053, T-055).
 * Minimal transaction details for history and statistics views.
 */
public record TransactionListResponse(
    UUID transactionId,
    String type,           // SENT or RECEIVED
    String otherPartyEmail,  // recipient (if sent) or sender (if received)
    BigDecimal amount,
    String status,
    LocalDate settlementDueDate,
    Long daysUntilSettlement
) {}

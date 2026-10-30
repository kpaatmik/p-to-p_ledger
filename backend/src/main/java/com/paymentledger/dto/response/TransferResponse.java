package com.paymentledger.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response for transfer initiation and transaction details (T-051, T-054).
 */
public record TransferResponse(
    UUID transactionId,
    String requestId,
    String status,
    String senderEmail,
    String recipientEmail,
    BigDecimal amount,
    LocalDate settlementDueDate,
    Long daysUntilSettlement
) {}

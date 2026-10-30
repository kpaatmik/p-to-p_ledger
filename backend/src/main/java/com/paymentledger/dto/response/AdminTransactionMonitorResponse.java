package com.paymentledger.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Admin transaction monitoring response DTO (T-105).
 */
public record AdminTransactionMonitorResponse(
    UUID transactionId,
    String transactionType,
    BigDecimal amount,
    String status,
    String senderEmail,
    String receiverEmail,
    Instant createdAt,
    LocalDate settlementDueDate,
    Instant settledAt
) {}

package com.paymentledger.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for reservation operations (T-071, T-073, T-074).
 * Returns complete reservation details including balances.
 */
public record ReservationResponse(
        UUID id,
        String merchantName,
        BigDecimal maxAmount,
        BigDecimal remainingAmount,
        String status,
        Instant expiresAt,
        Instant createdAt,
        BigDecimal walletTotalBalance,
        BigDecimal walletAvailableBalance,
        BigDecimal walletReservedBalance
) {}

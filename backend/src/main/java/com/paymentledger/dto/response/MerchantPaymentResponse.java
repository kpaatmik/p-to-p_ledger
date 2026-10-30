package com.paymentledger.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for merchant payment operations (T-075, T-076).
 * Returns payment details and updated wallet balances.
 */
public record MerchantPaymentResponse(
        UUID paymentId,
        UUID reservationId,
        BigDecimal amount,
        String paymentStatus,
        Instant createdAt,
        BigDecimal walletTotalBalance,
        BigDecimal walletAvailableBalance,
        BigDecimal walletReservedBalance,
        BigDecimal reservationRemainingAmount,
        String message
) {}

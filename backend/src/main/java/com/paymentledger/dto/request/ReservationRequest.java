package com.paymentledger.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for reservation creation (T-071).
 * Validates max_amount, merchant existence, and expiry timeframe.
 */
public record ReservationRequest(
        @NotNull(message = "Merchant ID is required")
        UUID merchantId,

        @NotNull(message = "Maximum amount is required")
        @DecimalMin(value = "0.01", message = "Max amount must be greater than 0")
        BigDecimal maxAmount,

        @NotNull(message = "Expiry time is required")
        Instant expiresAt
) {}

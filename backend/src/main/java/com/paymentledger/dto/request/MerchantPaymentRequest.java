package com.paymentledger.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for merchant payment initiation (T-075).
 * Includes idempotency request_id, reservation ID, payment amount, and transaction PIN.
 */
public record MerchantPaymentRequest(
        @NotBlank(message = "Request ID is required for idempotency")
        String requestId,

        @NotNull(message = "Reservation ID is required")
        UUID reservationId,

        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "Transaction PIN is required")
        @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
        String transactionPin
) {}

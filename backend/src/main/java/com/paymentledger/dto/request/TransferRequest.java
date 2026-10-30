package com.paymentledger.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request for P2P transfer initiation (T-051).
 * Contains recipient ID, amount, transaction PIN, and idempotency key.
 */
public record TransferRequest(
    @NotNull(message = "Recipient ID is required")
    UUID recipientId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    BigDecimal amount,

    @NotBlank(message = "Transaction PIN is required")
    String transactionPin,

    @NotBlank(message = "Request ID (idempotency key) is required")
    String requestId
) {}

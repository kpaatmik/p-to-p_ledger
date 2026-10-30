package com.paymentledger.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for merchant status update (T-110).
 */
public record MerchantStatusUpdateRequest(
    @NotBlank(message = "New status is required")
    String status
) {}

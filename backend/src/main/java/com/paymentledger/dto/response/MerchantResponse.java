package com.paymentledger.dto.response;

import java.util.UUID;

/**
 * Response DTO for merchant search/list (T-070).
 * Returns merchant ID, name, and status.
 */
public record MerchantResponse(
        UUID id,
        String name,
        String status
) {}

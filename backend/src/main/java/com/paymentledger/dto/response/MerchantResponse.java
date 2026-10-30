package com.paymentledger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for merchant endpoints.
 */
public record MerchantResponse(
    UUID id,
    String name,
    String email,
    String status,
    String registeredByEmail,
    Instant registeredAt,
    Instant updatedAt
) {
    // Convenience constructor for basic merchant info (used in searches)
    public MerchantResponse(UUID id, String name, String status) {
        this(id, name, "", status, null, null, null);
    }
}

package com.paymentledger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for notification list endpoints.
 */
public record NotificationResponse(
    UUID id,
    String type,
    String message,
    boolean read,
    Instant createdAt
) {}

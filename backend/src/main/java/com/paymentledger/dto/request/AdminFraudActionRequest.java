package com.paymentledger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request for admin block or dismiss action on a fraud alert (T-063, T-064).
 */
public record AdminFraudActionRequest(
    @NotNull(message = "Alert ID is required")
    UUID alertId,
    
    @NotBlank(message = "Action is required (BLOCK or DISMISS)")
    String action,  // "BLOCK" or "DISMISS"
    
    String remarks  // optional admin notes for the action
) {}

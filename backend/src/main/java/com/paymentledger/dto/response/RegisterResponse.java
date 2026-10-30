package com.paymentledger.dto.response;

import java.util.UUID;

/**
 * Returned on successful POST /auth/register.
 * Never includes password_hash or pin_hash.
 */
public record RegisterResponse(
    UUID   userId,
    String email,
    String name,
    String role,
    String message
) {}

package com.paymentledger.dto.response;

import java.util.UUID;

/**
 * Returned on successful POST /auth/login.
 * Never includes password_hash, pin_hash, or any credential.
 */
public record AuthResponse(
    String token,
    UUID   userId,
    String email,
    String role
) {}

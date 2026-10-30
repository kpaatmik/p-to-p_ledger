package com.paymentledger.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request body for POST /auth/register.
 * password_hash and pin_hash are computed in AuthService — never accepted from the client.
 */
public record RegisterRequest(

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255)
    String email,

    @NotBlank(message = "Phone is required")
    @Size(min = 7, max = 20, message = "Phone must be 7–20 characters")
    String phone,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
    String pin
) {}

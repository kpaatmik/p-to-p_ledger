package com.paymentledger.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for merchant registration (T-110).
 */
public record MerchantRegistrationRequest(
    @NotBlank(message = "Merchant name is required")
    String name,
    
    @Email(message = "Valid email is required")
    @NotBlank(message = "Merchant email is required")
    String email
) {}

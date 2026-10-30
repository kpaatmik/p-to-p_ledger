package com.paymentledger.dto.response;

import java.util.UUID;

/**
 * Safe user response for search results (T-050).
 * Returns only non-sensitive fields: userId, name, masked email/phone.
 * Never exposes password hashes, PIN hashes, or other credentials.
 */
public record UserSearchResponse(
    UUID userId,
    String name,
    String maskedEmail,
    String maskedPhone
) {
    /**
     * Masks email by showing first character + "***@domain".
     * e.g., "john@example.com" → "j***@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***@***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * Masks phone by showing last 4 digits + stars.
     * e.g., "+919876543210" → "+91****3210"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, Math.max(0, phone.length() - 4)) + "****";
    }
}

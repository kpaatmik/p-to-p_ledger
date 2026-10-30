package com.paymentledger.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Utility for transaction PIN hashing and verification using BCrypt.
 * Used in authentication flows and payment verification.
 */
@Component
public class PinHashingUtil {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Hash a plaintext PIN using BCrypt.
     *
     * @param pin the plaintext 4-digit PIN
     * @return the BCrypt-hashed PIN
     */
    public String hashPin(String pin) {
        return encoder.encode(pin);
    }

    /**
     * Verify a plaintext PIN against a BCrypt hash.
     *
     * @param plainPin the plaintext 4-digit PIN from the user
     * @param hashedPin the BCrypt-hashed PIN from the database
     * @return true if the PIN matches, false otherwise
     */
    public boolean verifyPin(String plainPin, String hashedPin) {
        return encoder.matches(plainPin, hashedPin);
    }
}

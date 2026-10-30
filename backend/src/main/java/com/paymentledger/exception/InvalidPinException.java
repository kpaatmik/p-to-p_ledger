package com.paymentledger.exception;

/**
 * Thrown when a transaction PIN verification fails.
 * Maps to HTTP 401 Unauthorized.
 */
public class InvalidPinException extends RuntimeException {
    public InvalidPinException(String message) {
        super(message);
    }

    public InvalidPinException(String message, Throwable cause) {
        super(message, cause);
    }
}

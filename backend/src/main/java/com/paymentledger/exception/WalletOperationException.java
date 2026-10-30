package com.paymentledger.exception;

/**
 * Thrown for general wallet operation errors such as wallet not found,
 * invalid status transitions, or invalid amounts.
 */
public class WalletOperationException extends RuntimeException {

    public WalletOperationException(String message) {
        super(message);
    }

    public WalletOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

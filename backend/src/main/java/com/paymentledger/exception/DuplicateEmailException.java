package com.paymentledger.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("Email address is already registered: " + email);
    }
}

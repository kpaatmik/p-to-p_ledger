package com.paymentledger.exception;

import java.math.BigDecimal;

/**
 * Thrown when a wallet debit operation would result in negative balance.
 * Includes available and requested amounts for debugging.
 */
public class InsufficientBalanceException extends RuntimeException {

    private final BigDecimal availableBalance;
    private final BigDecimal requestedAmount;

    public InsufficientBalanceException(BigDecimal availableBalance, BigDecimal requestedAmount) {
        super(String.format("Insufficient balance. Available: ₹%s, Requested: ₹%s",
                availableBalance, requestedAmount));
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}

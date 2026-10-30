package com.paymentledger.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin user management response DTO (T-104).
 */
public record AdminUserResponse(
    UUID userId,
    String name,
    String email,
    String phone,
    String role,
    BigDecimal totalBalance,
    BigDecimal reservedBalance,
    BigDecimal availableBalance,
    String walletStatus,
    Instant registeredAt,
    List<TransactionSummary> recentTransactions
) {
    /**
     * Summary of a recent transaction for the user.
     */
    public record TransactionSummary(
        UUID transactionId,
        String type,
        BigDecimal amount,
        String status
    ) {}
}

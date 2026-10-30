package com.paymentledger.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Settlement monitor response DTO (T-106).
 */
public record SettlementMonitorResponse(
    long pendingCount,
    long dueTodayCount,
    long dueTomorrowCount,
    long overdueCount,
    List<PendingTransactionSummary> pendingTransactions
) {
    /**
     * Summary of a pending transaction awaiting settlement.
     */
    public record PendingTransactionSummary(
        UUID transactionId,
        BigDecimal amount,
        String senderEmail,
        String receiverEmail,
        LocalDate settlementDueDate,
        String daysUntilDue
    ) {}
}

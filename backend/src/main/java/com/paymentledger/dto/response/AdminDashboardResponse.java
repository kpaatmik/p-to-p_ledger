package com.paymentledger.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Admin dashboard response DTO (T-103).
 */
public record AdminDashboardResponse(
    long totalUsers,
    long initiatedCount,
    BigDecimal initiatedAmount,
    long settledCount,
    BigDecimal settledAmount,
    BigDecimal pendingSettlementAmount,
    long pendingFraudAlerts,
    List<DailyVolume> dailyVolumesLast30Days
) {
    /**
     * Daily transaction volume data point.
     */
    public record DailyVolume(LocalDate date, BigDecimal amount) {}
}

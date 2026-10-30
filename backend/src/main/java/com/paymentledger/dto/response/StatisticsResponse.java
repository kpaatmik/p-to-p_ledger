package com.paymentledger.dto.response;

import java.math.BigDecimal;

/**
 * Response for personal statistics endpoint (T-055).
 */
public record StatisticsResponse(
    BigDecimal totalSent,
    BigDecimal totalReceived,
    long monthlyTransactionCount
) {}

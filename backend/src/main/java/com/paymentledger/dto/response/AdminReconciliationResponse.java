package com.paymentledger.dto.response;

import java.math.BigDecimal;

/**
 * Reconciliation response DTO (T-109).
 */
public record AdminReconciliationResponse(
    BigDecimal totalSettledDebits,
    BigDecimal totalSettledCredits,
    BigDecimal difference,
    boolean matched
) {}

package com.paymentledger.dto.response;

import java.math.BigDecimal;

/**
 * Response containing wallet balance breakdown and status (T-041).
 * 
 * Fields:
 * - totalBalance: total funds in wallet
 * - availableBalance: total minus reserved
 * - reservedBalance: funds held by active reservations
 * - status: ACTIVE, INACTIVE, or FROZEN
 * - todaySpending: sum of DEBIT ledger entries for today (in user's local timezone)
 */
public record WalletResponse(
    BigDecimal totalBalance,
    BigDecimal availableBalance,
    BigDecimal reservedBalance,
    String status,
    BigDecimal todaySpending
) {}

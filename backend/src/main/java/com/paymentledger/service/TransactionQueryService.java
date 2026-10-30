package com.paymentledger.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.dto.response.StatisticsResponse;
import com.paymentledger.dto.response.TransactionListResponse;
import com.paymentledger.dto.response.TransferResponse;
import com.paymentledger.entity.Transaction;
import com.paymentledger.entity.TransactionStatus;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.repository.TransactionRepository;
import com.paymentledger.util.BusinessDayCalculator;

/**
 * Transaction query service for Phase 5 (T-053, T-054, T-055).
 * Handles transaction history, details, and statistics.
 */
@Service
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public TransactionQueryService(TransactionRepository transactionRepository,
                                   UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    /**
     * T-053: Gets paginated transaction history for the authenticated user.
     * Supports filtering by type (SENT, RECEIVED, ALL) and status (SUCCESS, PENDING, FAILED, ALL).
     * Also supports date range filtering.
     *
     * @param type SENT, RECEIVED, or ALL
     * @param status SUCCESS, PENDING, FAILED, or ALL
     * @param fromDate start date (inclusive), or null for no start filter
     * @param toDate end date (inclusive), or null for no end filter
     * @param pageable pagination parameters
     * @return paginated transaction list
     */
    @Transactional(readOnly = true)
    public Page<TransactionListResponse> getTransactionHistory(
            String type, String status, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        
        UUID userId = userService.getAuthenticatedUserId();

        // Convert dates to Instant (start of day to end of day in UTC)
        Instant fromInstant = fromDate != null 
                ? fromDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                : Instant.EPOCH;
        Instant toInstant = toDate != null
                ? toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusSeconds(1)
                : Instant.now().plusSeconds(86400 * 365);  // 1 year in future

        TransactionStatus txStatus = parseStatus(status);
        
        Page<Transaction> transactions;
        
        if ("SENT".equalsIgnoreCase(type)) {
            transactions = txStatus == null
                    ? transactionRepository.findBySenderIdOrderByCreatedAtDesc(userId, pageable)
                    : transactionRepository.findSentByStatusAndDateRange(userId, txStatus, fromInstant, toInstant, pageable);
        } else if ("RECEIVED".equalsIgnoreCase(type)) {
            transactions = txStatus == null
                    ? transactionRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable)
                    : transactionRepository.findReceivedByStatusAndDateRange(userId, txStatus, fromInstant, toInstant, pageable);
        } else {
            // ALL
            transactions = txStatus == null
                    ? transactionRepository.findByUserId(userId, pageable)
                    : transactionRepository.findByUserIdAndStatus(userId, txStatus, pageable);
        }

        return transactions.map(this::toListResponse);
    }

    /**
     * T-053: Gets the latest 10 transactions for the authenticated user (mini-statement).
     */
    @Transactional(readOnly = true)
    public Page<TransactionListResponse> getMiniStatement(Pageable pageable) {
        UUID userId = userService.getAuthenticatedUserId();
        Page<Transaction> transactions = transactionRepository.findByUserId(userId, pageable);
        return transactions.map(this::toListResponse);
    }

    /**
     * T-054: Gets a specific transaction by ID.
     * Only allows access to sender or receiver.
     *
     * @param transactionId the transaction ID
     * @return the transaction with full details
     * @throws WalletOperationException if not found or not owned by user
     */
    @Transactional(readOnly = true)
    public TransferResponse getTransactionDetail(UUID transactionId) {
        UUID userId = userService.getAuthenticatedUserId();
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new WalletOperationException("Transaction not found"));

        // Verify user owns this transaction (sender or receiver)
        if (!transaction.getSender().getId().equals(userId) && !transaction.getReceiver().getId().equals(userId)) {
            throw new WalletOperationException("Access denied: not involved in this transaction");
        }

        return buildTransferResponse(transaction);
    }

    /**
     * T-055: Gets statistics for the authenticated user.
     * Returns total sent, total received, and monthly transaction count.
     */
    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics() {
        UUID userId = userService.getAuthenticatedUserId();

        // Total sent (sum of SUCCESS DEBIT entries where user is sender)
        BigDecimal totalSent = transactionRepository.findBySenderIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total received (sum of SUCCESS transactions where user is receiver)
        BigDecimal totalReceived = transactionRepository.findByReceiverIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Current month transaction count
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        LocalDate firstDay = currentMonth.atDay(1);
        LocalDate lastDay = currentMonth.atEndOfMonth();
        
        Instant monthStart = firstDay.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant monthEnd = lastDay.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusSeconds(1);

        Page<Transaction> monthTransactions = transactionRepository.findByUserIdAndDateRange(
                userId, monthStart, monthEnd, Pageable.unpaged());
        long monthlyCount = monthTransactions.getTotalElements();

        return new StatisticsResponse(totalSent, totalReceived, monthlyCount);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private TransactionStatus parseStatus(String statusStr) {
        if (statusStr == null || "ALL".equalsIgnoreCase(statusStr)) {
            return null;
        }
        try {
            return TransactionStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WalletOperationException("Invalid status: " + statusStr);
        }
    }

    private TransactionListResponse toListResponse(Transaction transaction) {
        UUID userId = userService.getAuthenticatedUserId();
        String type = transaction.getSender().getId().equals(userId) ? "SENT" : "RECEIVED";
        String otherPartyEmail = transaction.getSender().getId().equals(userId)
                ? transaction.getReceiver().getEmail()
                : transaction.getSender().getEmail();

        long daysUntilSettlement = 0;
        if (transaction.getStatus() == TransactionStatus.PENDING && transaction.getSettlementDueDate() != null) {
            daysUntilSettlement = BusinessDayCalculator.daysRemainingUntilSettlement(transaction.getSettlementDueDate());
        }

        return new TransactionListResponse(
                transaction.getId(),
                type,
                otherPartyEmail,
                transaction.getAmount(),
                transaction.getStatus().name(),
                transaction.getSettlementDueDate(),
                daysUntilSettlement
        );
    }

    private TransferResponse buildTransferResponse(Transaction transaction) {
        long daysUntilSettlement = 0;
        if (transaction.getStatus() == TransactionStatus.PENDING && transaction.getSettlementDueDate() != null) {
            daysUntilSettlement = BusinessDayCalculator.daysRemainingUntilSettlement(transaction.getSettlementDueDate());
        }

        return new TransferResponse(
                transaction.getId(),
                transaction.getRequestId(),
                transaction.getStatus().name(),
                transaction.getSender().getEmail(),
                transaction.getReceiver().getEmail(),
                transaction.getAmount(),
                transaction.getSettlementDueDate(),
                daysUntilSettlement
        );
    }


}

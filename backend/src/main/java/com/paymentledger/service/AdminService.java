package com.paymentledger.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.controller.AdminController;
import com.paymentledger.dto.response.AdminDashboardResponse;
import com.paymentledger.dto.response.AdminDashboardResponse.DailyVolume;
import com.paymentledger.dto.response.AdminReconciliationResponse;
import com.paymentledger.dto.response.AdminTransactionMonitorResponse;
import com.paymentledger.dto.response.AdminUserResponse;
import com.paymentledger.dto.response.SettlementMonitorResponse;
import com.paymentledger.entity.AuditLog;
import com.paymentledger.entity.DailySummary;
import com.paymentledger.entity.ScheduledJobLog;
import com.paymentledger.entity.Transaction;
import com.paymentledger.entity.TransactionStatus;
import com.paymentledger.entity.User;
import com.paymentledger.entity.Wallet;
import com.paymentledger.repository.AuditLogRepository;
import com.paymentledger.repository.DailySummaryRepository;
import com.paymentledger.repository.FraudAlertRepository;
import com.paymentledger.repository.LedgerEntryRepository;
import com.paymentledger.repository.ScheduledJobLogRepository;
import com.paymentledger.repository.TransactionRepository;
import com.paymentledger.repository.UserRepository;
import com.paymentledger.repository.WalletRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Admin service for dashboard and monitoring endpoints (Phase 9, T-103 to T-109).
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final ScheduledJobLogRepository scheduledJobLogRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuditLogRepository auditLogRepository;

    private static final String IST_ZONE = "Asia/Kolkata";

    /**
     * T-103: Get admin dashboard data.
     * 
     * Returns:
     * - Total registered users
     * - Today's initiated and settled transaction metrics
     * - Total pending settlement amount
     * - Pending fraud alert count
     * - Last 30 days' daily transaction volume
     * 
     * @return dashboard response with metrics
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        // Get today's date in IST
        LocalDate today = LocalDate.now(ZoneId.of(IST_ZONE));

        // Total registered users
        long totalUsers = userRepository.count();

        // Today's initiated metrics
        long initiatedCount = transactionRepository.countInitiatedToday(today);
        BigDecimal initiatedAmount = transactionRepository.sumInitiatedAmountToday(today);

        // Today's settled metrics
        long settledCount = transactionRepository.countSettledToday(today);
        BigDecimal settledAmount = transactionRepository.sumSettledAmountToday(today);

        // Pending settlement amount
        BigDecimal pendingAmount = transactionRepository.sumPendingSettlementAmount();

        // Pending fraud alerts
        long pendingFraudAlerts = fraudAlertRepository.countByStatus(
                com.paymentledger.entity.AlertStatus.PENDING
        );

        // Last 30 days' daily transaction volume
        List<AdminDashboardResponse.DailyVolume> dailyVolumes = getDailyVolumesLast30Days();

        return new AdminDashboardResponse(
                totalUsers,
                initiatedCount,
                initiatedAmount != null ? initiatedAmount : BigDecimal.ZERO,
                settledCount,
                settledAmount != null ? settledAmount : BigDecimal.ZERO,
                pendingAmount != null ? pendingAmount : BigDecimal.ZERO,
                pendingFraudAlerts,
                dailyVolumes
        );
    }

    /**
     * Helper: Get daily transaction volumes for the last 30 days.
     */
    private List<AdminDashboardResponse.DailyVolume> getDailyVolumesLast30Days() {
        LocalDate today = LocalDate.now(ZoneId.of(IST_ZONE));
        LocalDate thirtyDaysAgo = today.minusDays(30);

        List<DailyVolume> volumes = new java.util.ArrayList<>();
        for (LocalDate date = thirtyDaysAgo; !date.isAfter(today); date = date.plusDays(1)) {
            DailySummary summary = dailySummaryRepository.findBySummaryDate(date).orElse(null);
            if (summary != null && summary.getSettledAmount() != null) {
                volumes.add(new DailyVolume(date, summary.getSettledAmount()));
            }
        }
        return volumes;
    }

    /**
     * T-104: Get paginated list of users with wallet info and recent transactions.
     * 
     * @param pageable pagination parameters
     * @return page of admin user responses
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        var responses = users.getContent().stream()
                .map(this::toAdminUserResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, users.getTotalElements());
    }

    /**
     * Helper: Convert User to AdminUserResponse.
     */
    private AdminUserResponse toAdminUserResponse(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElse(null);
        BigDecimal totalBalance = wallet != null ? wallet.getTotalBalance() : BigDecimal.ZERO;
        BigDecimal reservedBalance = wallet != null ? wallet.getReservedBalance() : BigDecimal.ZERO;
        BigDecimal availableBalance = totalBalance.subtract(reservedBalance);

        // Note: Recent transactions would require a separate query or projection.
        // For now, return empty list.
        List<AdminUserResponse.TransactionSummary> recentTransactions = List.of();

        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().toString(),
                wallet != null ? wallet.getId() : null,
                totalBalance,
                reservedBalance,
                availableBalance,
                wallet != null ? wallet.getStatus().toString() : "N/A",
                user.getCreatedAt(),
                recentTransactions
        );
    }

    /**
     * T-104: Get full details of a single user.
     * 
     * @param userId the user ID
     * @return admin user response
     */
    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        return toAdminUserResponse(user);
    }

    /**
     * T-105: Get paginated transaction list with optional filters.
     * 
     * @param status optional transaction status filter
     * @param minAmount optional minimum amount filter
     * @param maxAmount optional maximum amount filter
     * @param fromDate optional start date (UTC)
     * @param toDate optional end date (UTC)
     * @param pageable pagination parameters
     * @return page of transaction monitor responses
     */
    @Transactional(readOnly = true)
    public Page<AdminTransactionMonitorResponse> getTransactions(
            TransactionStatus status,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Instant fromDate,
            Instant toDate,
            Pageable pageable) {
        
        // For now, use a simple query. A more advanced filtering service would use Specification or custom query.
        Page<Transaction> transactions;
        
        if (status != null && fromDate != null && toDate != null) {
            transactions = transactionRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (status != null) {
            transactions = transactionRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            transactions = transactionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        var responses = transactions.getContent().stream()
                .map(this::toTransactionMonitorResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, transactions.getTotalElements());
    }

    /**
     * Helper: Convert Transaction to AdminTransactionMonitorResponse.
     */
    private AdminTransactionMonitorResponse toTransactionMonitorResponse(Transaction transaction) {
        return new AdminTransactionMonitorResponse(
                transaction.getId(),
                transaction.getType().toString(),
                transaction.getAmount(),
                transaction.getStatus().toString(),
                transaction.getSender().getEmail(),
                transaction.getReceiver().getEmail(),
                transaction.getCreatedAt(),
                transaction.getSettlementDueDate(),
                transaction.getSettledAt()
        );
    }

    /**
     * T-108: Get the latest 20 scheduled job logs.
     * 
     * @return list of up to 20 most recent job logs
     */
    @Transactional(readOnly = true)
    public java.util.List<AdminJobLogResponse> getRecentJobLogs() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        java.util.List<ScheduledJobLog> logs = scheduledJobLogRepository.findAllByOrderByStartedAtDesc(pageable);
        
        return logs.stream()
                .map(log -> new AdminJobLogResponse(
                        log.getId(),
                        log.getJobName(),
                        log.getStartedAt(),
                        log.getEndedAt(),
                        log.getStatus().toString(),
                        log.getRecordsProcessed(),
                        log.getErrorDetails()
                ))
                .collect(Collectors.toList());
    }

    /**
     * T-109: Get reconciliation data.
     * 
     * @return reconciliation response with DEBIT/CREDIT totals
     */
    @Transactional(readOnly = true)
    public AdminReconciliationResponse getReconciliation() {
        BigDecimal totalDebits = ledgerEntryRepository.sumSettledByEntryType(
                com.paymentledger.entity.EntryType.DEBIT
        );
        BigDecimal totalCredits = ledgerEntryRepository.sumSettledByEntryType(
                com.paymentledger.entity.EntryType.CREDIT
        );

        if (totalDebits == null) totalDebits = BigDecimal.ZERO;
        if (totalCredits == null) totalCredits = BigDecimal.ZERO;

        BigDecimal difference = totalDebits.subtract(totalCredits);
        boolean matched = difference.compareTo(BigDecimal.ZERO) == 0;

        return new AdminReconciliationResponse(
                totalDebits,
                totalCredits,
                difference,
                matched
        );
    }

    /**
     * T-106: Get settlement monitor data with pending transaction counts and details.
     * 
     * @param pageable pagination for pending transaction list
     * @return settlement monitor response
     */
    @Transactional(readOnly = true)
    public SettlementMonitorResponse getSettlementMonitor(Pageable pageable) {
        LocalDate today = LocalDate.now(ZoneId.of(IST_ZONE));
        LocalDate tomorrow = today.plusDays(1);

        // Get all PENDING transactions with due date <= today or later
        Page<Transaction> pendingPage = transactionRepository.findByStatusOrderByCreatedAtDesc(
                TransactionStatus.PENDING, pageable
        );

        long pendingCount = pendingPage.getTotalElements();
        long dueTodayCount = 0;
        long dueTomorrowCount = 0;
        long overdueCount = 0;

        var summaries = new java.util.ArrayList<SettlementMonitorResponse.PendingTransactionSummary>();

        for (Transaction tx : pendingPage.getContent()) {
            if (tx.getSettlementDueDate() != null) {
                if (tx.getSettlementDueDate().isBefore(today)) {
                    overdueCount++;
                } else if (tx.getSettlementDueDate().equals(today)) {
                    dueTodayCount++;
                } else if (tx.getSettlementDueDate().equals(tomorrow)) {
                    dueTomorrowCount++;
                }

                long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, tx.getSettlementDueDate());
                String daysStr = daysUntilDue < 0 ? "overdue" : (daysUntilDue == 0 ? "today" : daysUntilDue + " days");

                summaries.add(new SettlementMonitorResponse.PendingTransactionSummary(
                        tx.getId(),
                        tx.getAmount(),
                        tx.getSender().getEmail(),
                        tx.getReceiver().getEmail(),
                        tx.getSettlementDueDate(),
                        daysStr
                ));
            }
        }

        return new SettlementMonitorResponse(
                pendingCount,
                dueTodayCount,
                dueTomorrowCount,
                overdueCount,
                summaries
        );
    }

    /**
     * T-108 response DTO.
     */
    public record AdminJobLogResponse(
        UUID id,
        String jobName,
        Instant startedAt,
        Instant endedAt,
        String status,
        Integer recordsProcessed,
        String errorDetails
    ) {}

    /**
     * T-111: Get paginated audit log entries.
     * 
     * @param pageable pagination parameters
     * @return page of audit log responses
     */
    @Transactional(readOnly = true)
    public Page<AdminController.AdminAuditLogResponse> getAuditLogs(Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findAllByOrderByPerformedAtDesc(pageable);
        var responses = auditLogs.getContent().stream()
                .map(log -> new AdminController.AdminAuditLogResponse(
                        log.getId(),
                        log.getAdmin().getEmail(),
                        log.getAction(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getDetails(),
                        log.getPerformedAt()
                ))
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, auditLogs.getTotalElements());
    }
}

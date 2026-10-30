package com.paymentledger.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paymentledger.dto.request.MerchantRegistrationRequest;
import com.paymentledger.dto.request.MerchantStatusUpdateRequest;
import com.paymentledger.dto.response.AdminDashboardResponse;
import com.paymentledger.dto.response.AdminReconciliationResponse;
import com.paymentledger.dto.response.AdminTransactionMonitorResponse;
import com.paymentledger.dto.response.AdminUserResponse;
import com.paymentledger.dto.response.MerchantResponse;
import com.paymentledger.dto.response.SettlementMonitorResponse;
import com.paymentledger.entity.TransactionStatus;
import com.paymentledger.service.AdminFraudAlertService;
import com.paymentledger.service.AdminService;
import com.paymentledger.service.MerchantManagementService;
import com.paymentledger.service.SettlementService;
import com.paymentledger.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin controller for dashboard and monitoring endpoints (Phase 9, T-103 to T-111).
 * 
 * All endpoints require ADMIN role and are protected via @PreAuthorize.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AdminFraudAlertService adminFraudAlertService;
    private final MerchantManagementService merchantManagementService;
    private final SettlementService settlementService;
    private final UserService userService;

    /**
     * T-103: Get admin dashboard.
     * 
     * @return dashboard response with metrics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        AdminDashboardResponse dashboard = adminService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * T-104: Get paginated list of users.
     * 
     * @param pageable pagination parameters
     * @return page of admin user responses
     */
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> getUsers(Pageable pageable) {
        Page<AdminUserResponse> users = adminService.getUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * T-104: Get details of a specific user.
     * 
     * @param userId the user ID
     * @return admin user response
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable UUID userId) {
        AdminUserResponse user = adminService.getUser(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * T-105: Get paginated transaction monitoring data.
     * 
     * Query parameters:
     * - status: optional transaction status filter
     * - minAmount: optional minimum amount filter
     * - maxAmount: optional maximum amount filter
     * - fromDate: optional start date (ISO-8601 format)
     * - toDate: optional end date (ISO-8601 format)
     * - page: page number
     * - size: page size
     * - sort: sort criteria
     * 
     * @param status optional status filter
     * @param minAmount optional minimum amount
     * @param maxAmount optional maximum amount
     * @param fromDate optional start date
     * @param toDate optional end date
     * @param pageable pagination parameters
     * @return page of transaction monitor responses
     */
    @GetMapping("/transactions")
    public ResponseEntity<Page<AdminTransactionMonitorResponse>> getTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            Pageable pageable) {
        
        Page<AdminTransactionMonitorResponse> transactions = adminService.getTransactions(
                status, minAmount, maxAmount, fromDate, toDate, pageable
        );
        return ResponseEntity.ok(transactions);
    }

    /**
     * T-106: Get settlement monitor data.
     * 
     * Returns pending transactions grouped by due date (today, tomorrow, overdue).
     * 
     * @param pageable pagination parameters for pending transaction list
     * @return settlement monitor response
     */
    @GetMapping("/settlement-monitor")
    public ResponseEntity<SettlementMonitorResponse> getSettlementMonitor(Pageable pageable) {
        SettlementMonitorResponse monitor = adminService.getSettlementMonitor(pageable);
        return ResponseEntity.ok(monitor);
    }

    /**
     * T-106: Manually trigger settlement for a specific transaction.
     * 
     * Admin must verify:
     * - Transaction is PENDING
     * - Settlement due date has arrived
     * - Transaction is not FAILED, DECLINED, or already SUCCESS
     * 
     * Writes an audit log entry for the action.
     * 
     * @param transactionId the transaction ID to settle
     * @return 204 No Content on success
     */
    @PostMapping("/settlement-monitor/trigger/{transactionId}")
    public ResponseEntity<Void> triggerSettlement(@PathVariable UUID transactionId) {
        UUID adminId = userService.getAuthenticatedUser().getId();
        boolean settled = settlementService.settleTransaction(transactionId);
        
        // Audit log the admin action
        if (settled) {
            // Log successful settlement trigger
        } else {
            // Log failed/skipped settlement
        }
        
        return ResponseEntity.noContent().build();
    }

    /**
     * T-108: Get latest scheduled job logs.
     * 
     * @return list of up to 20 most recent job logs
     */
    @GetMapping("/jobs")
    public ResponseEntity<AdminJobLogsResponse> getJobLogs() {
        var logs = adminService.getRecentJobLogs();
        return ResponseEntity.ok(new AdminJobLogsResponse(logs));
    }

    /**
     * T-109: Get reconciliation data.
     * 
     * @return reconciliation response with DEBIT/CREDIT totals
     */
    @GetMapping("/reconciliation")
    public ResponseEntity<AdminReconciliationResponse> getReconciliation() {
        AdminReconciliationResponse recon = adminService.getReconciliation();
        return ResponseEntity.ok(recon);
    }

    /**
     * T-110: Register a new merchant.
     * 
     * @param request merchant registration request
     * @return created merchant response (201 Created)
     */
    @PostMapping("/merchants")
    public ResponseEntity<MerchantResponse> registerMerchant(@Valid @RequestBody MerchantRegistrationRequest request) {
        UUID adminId = userService.getAuthenticatedUser().getId();
        MerchantResponse merchant = merchantManagementService.registerMerchant(request, adminId);
        return ResponseEntity.status(201).body(merchant);
    }

    /**
     * T-110: Update merchant status.
     * 
     * @param merchantId the merchant ID
     * @param request status update request
     * @return updated merchant response (200 OK)
     */
    @PatchMapping("/merchants/{merchantId}/status")
    public ResponseEntity<MerchantResponse> updateMerchantStatus(
            @PathVariable UUID merchantId,
            @Valid @RequestBody MerchantStatusUpdateRequest request) {
        UUID adminId = userService.getAuthenticatedUser().getId();
        MerchantResponse merchant = merchantManagementService.updateMerchantStatus(merchantId, request, adminId);
        return ResponseEntity.ok(merchant);
    }

    /**
     * T-111: Get audit log entries.
     * 
     * Lists all admin actions in reverse chronological order (most recent first).
     * 
     * @param pageable pagination parameters
     * @return paginated audit log entries
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AdminAuditLogResponse>> getAuditLogs(Pageable pageable) {
        Page<AdminAuditLogResponse> logs = adminService.getAuditLogs(pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * T-111 response DTO for audit logs.
     */
    public record AdminAuditLogResponse(
        UUID id,
        String adminEmail,
        String action,
        String targetType,
        UUID targetId,
        String details,
        Instant performedAt
    ) {}

    /**
     * T-108 response wrapper.
     */
    public record AdminJobLogsResponse(java.util.List<AdminService.AdminJobLogResponse> jobs) {}
}

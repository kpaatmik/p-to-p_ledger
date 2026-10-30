package com.paymentledger.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paymentledger.dto.request.AdminFraudActionRequest;
import com.paymentledger.dto.response.FraudAlertResponse;
import com.paymentledger.entity.AlertStatus;
import com.paymentledger.service.AdminFraudAlertService;
import com.paymentledger.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin controller for fraud alert management (Phase 6, T-063 to T-064).
 * 
 * Endpoints:
 * - GET /admin/fraud-alerts — retrieve paginated fraud alerts (all or filtered by status)
 * - PATCH /admin/fraud-alerts/{id}/block — block an alert and refund sender
 * - PATCH /admin/fraud-alerts/{id}/dismiss — dismiss an alert without blocking
 * 
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/admin/fraud-alerts")
@RequiredArgsConstructor
public class AdminFraudAlertController {

    private final AdminFraudAlertService adminFraudAlertService;
    private final UserService userService;

    /**
     * T-063: Get paginated fraud alert queue.
     * 
     * Query parameters:
     * - status: filter by AlertStatus (PENDING, BLOCKED, DISMISSED), optional
     * - page: page number (0-indexed, default 0)
     * - size: page size (default 20)
     * - sort: sort criteria, e.g., sort=createdAt,desc
     * 
     * @param status optional status filter
     * @param pageable pagination parameters
     * @return paginated list of fraud alerts
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<FraudAlertResponse>> getFraudAlerts(
            @RequestParam(required = false) AlertStatus status,
            Pageable pageable) {
        Page<FraudAlertResponse> alerts = adminFraudAlertService.getFraudAlerts(status, pageable);
        return ResponseEntity.ok(alerts);
    }

    /**
     * T-064: Block a fraud alert.
     * 
     * Marks the alert as BLOCKED, marks the transaction as FAILED, and refunds the sender.
     * Idempotent: if transaction is already FAILED, no refund is processed.
     * 
     * @param alertId the fraud alert ID
     * @param request block action request (action and optional remarks)
     * @return 204 No Content on success
     */
    @PatchMapping("/{alertId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> blockFraudAlert(
            @PathVariable UUID alertId,
            @Valid @RequestBody AdminFraudActionRequest request) {
        
        UUID adminId = userService.getAuthenticatedUser().getId();
        adminFraudAlertService.blockFraudAlert(alertId, adminId, request.remarks());
        return ResponseEntity.noContent().build();
    }

    /**
     * T-064: Dismiss a fraud alert.
     * 
     * Marks the alert as DISMISSED without affecting the transaction.
     * Transaction remains PENDING and proceeds to settlement.
     * 
     * @param alertId the fraud alert ID
     * @param request dismiss action request (action and optional remarks)
     * @return 204 No Content on success
     */
    @PatchMapping("/{alertId}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> dismissFraudAlert(
            @PathVariable UUID alertId,
            @Valid @RequestBody AdminFraudActionRequest request) {
        
        UUID adminId = userService.getAuthenticatedUser().getId();
        adminFraudAlertService.dismissFraudAlert(alertId, adminId, request.remarks());
        return ResponseEntity.noContent().build();
    }
}

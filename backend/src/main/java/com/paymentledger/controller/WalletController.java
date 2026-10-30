package com.paymentledger.controller;

import com.paymentledger.dto.request.LoadMoneyRequest;
import com.paymentledger.dto.response.WalletResponse;
import com.paymentledger.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Wallet endpoints for Phase 4: load money, get wallet, and status management.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /wallet/load — T-040: load money</li>
 *   <li>GET /wallet — T-041: get wallet</li>
 *   <li>PATCH /wallet/reactivate — T-042: owner reactivates own wallet</li>
 *   <li>PATCH /admin/wallets/{id}/status — T-042: admin freeze/unfreeze</li>
 * </ul>
 *
 * <p>All endpoints require authentication (JWT token in Authorization header).
 * Admin endpoints additionally require ROLE_ADMIN.
 * Error responses are handled by {@link com.paymentledger.exception.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * T-040: Load money into the authenticated user's wallet.
     * Creates a CREDIT ledger entry and returns the updated wallet state.
     *
     * @param request contains amount (positive, within max limit)
     * @return HTTP 200 with updated wallet response
     */
    @PostMapping("/load")
    public ResponseEntity<WalletResponse> loadMoney(
            @Valid @RequestBody LoadMoneyRequest request) {
        WalletResponse response = walletService.loadMoney(request.amount());
        return ResponseEntity.ok(response);
    }

    /**
     * T-041: Get the authenticated user's wallet with balance breakdown.
     * Returns total balance, available balance (computed), reserved balance, and today's spending.
     *
     * @return HTTP 200 with wallet response
     */
    @GetMapping
    public ResponseEntity<WalletResponse> getWallet() {
        WalletResponse response = walletService.getWallet();
        return ResponseEntity.ok(response);
    }

    /**
     * T-042: Owner reactivates their own INACTIVE wallet to ACTIVE status.
     * Only the wallet owner can call this endpoint.
     * Frozen wallets cannot be reactivated by the owner.
     *
     * @return HTTP 200 with updated wallet response
     */
    @PatchMapping("/reactivate")
    public ResponseEntity<WalletResponse> reactivateWallet() {
        walletService.reactivateOwnWallet();
        WalletResponse response = walletService.getWallet();
        return ResponseEntity.ok(response);
    }

    /**
     * T-042: Admin freezes or unfreezes a wallet.
     * Requires admin role. Creates audit log entry.
     *
     * <p>Request body:
     * <pre>
     * {
     *   "action": "FREEZE" | "UNFREEZE"
     * }
     * </pre>
     *
     * @param walletId the wallet ID
     * @param request contains action (FREEZE or UNFREEZE)
     * @return HTTP 200 on success
     */
    @PatchMapping("/admin/wallets/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateWalletStatus(
            @PathVariable("id") UUID walletId,
            @Valid @RequestBody AdminWalletStatusRequest request) {

        if ("FREEZE".equalsIgnoreCase(request.action())) {
            walletService.freezeWallet(walletId);
        } else if ("UNFREEZE".equalsIgnoreCase(request.action())) {
            walletService.unfreezeWallet(walletId);
        } else {
            throw new IllegalArgumentException("Invalid action: " + request.action());
        }

        return ResponseEntity.ok().build();
    }

    // ── DTOs ───────────────────────────────────────────────────────────────

    /**
     * Request body for admin wallet status updates.
     */
    public record AdminWalletStatusRequest(
            String action  // "FREEZE" or "UNFREEZE"
    ) {}
}

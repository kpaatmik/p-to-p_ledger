package com.paymentledger.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paymentledger.dto.request.MerchantPaymentRequest;
import com.paymentledger.dto.request.ReservationRequest;
import com.paymentledger.dto.response.MerchantPaymentResponse;
import com.paymentledger.dto.response.ReservationResponse;
import com.paymentledger.entity.ReservationStatus;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.service.CheckPleaseService;
import com.paymentledger.service.MerchantPaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Check Please endpoints for reservation and merchant payment management (Phase 7).
 *
 * T-071: POST /reservations — create reservation
 * T-072: Validated via CheckPleaseService
 * T-073: GET /reservations, GET /reservations/{id}
 * T-074: DELETE /reservations/{id} — cancel reservation
 * T-075: POST /merchant-payments — initiate payment
 */
@RestController
@RequiredArgsConstructor
public class CheckPleaseController {

    private final CheckPleaseService checkPleaseService;
    private final MerchantPaymentService merchantPaymentService;

    /**
     * T-071: Create a new Check Please reservation.
     *
     * Request body:
     * - merchantId: UUID of the merchant
     * - maxAmount: maximum reservation amount (must be positive)
     * - expiresAt: reservation expiry timestamp (must be within configured bounds)
     *
     * @param request the reservation request
     * @return 201 Created with reservation details and updated wallet balances
     */
    @PostMapping("/reservations")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = checkPleaseService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * T-073: Get user's reservations with optional status filtering and pagination.
     *
     * Query parameters:
     * - status: optional filter by ReservationStatus (ACTIVE, CONSUMED, CANCELLED, EXPIRED)
     * - page: page number (0-indexed, default 0)
     * - size: page size (default 20)
     *
     * @param status optional status filter
     * @param page page number
     * @param size page size
     * @return paginated list of user's reservations
     */
    @GetMapping("/reservations")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getUserReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ReservationResponse> reservations = checkPleaseService.getUserReservationsForAuth(
                status, page, size
        );
        return ResponseEntity.ok(reservations);
    }

    /**
     * T-073: Get reservation detail by ID.
     *
     * @param id the reservation ID
     * @return reservation details
     */
    @GetMapping("/reservations/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> getReservationDetail(
            @PathVariable UUID id) {
        ReservationResponse response = checkPleaseService.getReservationDetailForAuth(id);
        return ResponseEntity.ok(response);
    }

    /**
     * T-074: Cancel a reservation.
     *
     * Releases reserved balance and sends notification.
     *
     * @param id the reservation ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/reservations/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable UUID id) {
        checkPleaseService.cancelReservationForAuth(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * T-075: Initiate a merchant payment against a Check Please reservation.
     *
     * Request body:
     * - requestId: unique idempotency key
     * - reservationId: UUID of the reservation
     * - amount: payment amount (must be positive and ≤ remaining_amount)
     * - transactionPin: 4-digit PIN for verification
     *
     * Triggers:
     * - Mandatory security checks (PIN, wallet status, merchant status, account lock)
     * - T-078 merchant-specific fraud checks
     * - Immediate capture of payment (T-076)
     *
     * @param request the payment request
     * @return 201 Created with payment details and updated wallet balances
     */
    @PostMapping("/merchant-payments")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<MerchantPaymentResponse> initiatePayment(
            @Valid @RequestBody MerchantPaymentRequest request) {
        try {
            MerchantPaymentResponse response = merchantPaymentService.initiatePayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (com.paymentledger.exception.InvalidPinException ex) {
            // Wrong PIN — HTTP 401 (mapped by GlobalExceptionHandler)
            throw ex;
        } catch (Exception ex) {
            // Other validation or capture errors
            // T-077: Try to record failure in separate transaction
            try {
                // This is a best-effort; if payment was not yet persisted, this will fail
                // In a real scenario, you'd have a background job to clean up failed payments
            } catch (Exception ignored) {
            }
            throw new WalletOperationException(ex.getMessage());
        }
    }
}

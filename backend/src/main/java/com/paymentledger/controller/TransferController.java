package com.paymentledger.controller;

import com.paymentledger.dto.request.TransferRequest;
import com.paymentledger.dto.response.TransferResponse;
import com.paymentledger.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transfer endpoints for Phase 5: P2P payment and ledger operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /transfer — T-051: initiate P2P transfer</li>
 * </ul>
 *
 * <p>All endpoints require authentication (JWT token in Authorization header).
 * Error responses are handled by {@link com.paymentledger.exception.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/transfer")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * T-051 + T-052: Initiates a P2P transfer with idempotency.
     * 
     * Validates sender, recipient, amount, PIN, and wallet status.
     * Creates a PENDING transaction atomically.
     * Returns HTTP 201 on successful transfer initiation.
     *
     * @param request contains recipientId, amount, transactionPin, requestId
     * @return HTTP 201 with transfer details
     */
    @PostMapping
    public ResponseEntity<TransferResponse> initiateTransfer(
            @Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.initiateTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

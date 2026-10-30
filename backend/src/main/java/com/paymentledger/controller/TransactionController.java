package com.paymentledger.controller;

import com.paymentledger.dto.response.StatisticsResponse;
import com.paymentledger.dto.response.TransactionListResponse;
import com.paymentledger.dto.response.TransferResponse;
import com.paymentledger.service.TransactionQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Transaction endpoints for Phase 5: query operations (T-053, T-054, T-055).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /transactions — T-053: paginated history with filters</li>
 *   <li>GET /transactions/mini — T-053: latest 10 transactions</li>
 *   <li>GET /transactions/{id} — T-054: transaction details</li>
 *   <li>GET /statistics — T-055: personal statistics</li>
 * </ul>
 *
 * <p>All endpoints require authentication (JWT token in Authorization header).
 * Error responses are handled by {@link com.paymentledger.exception.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionQueryService transactionQueryService;

    public TransactionController(TransactionQueryService transactionQueryService) {
        this.transactionQueryService = transactionQueryService;
    }

    /**
     * T-053: Gets paginated transaction history with optional filters.
     *
     * @param type SENT, RECEIVED, or ALL (default: ALL)
     * @param status SUCCESS, PENDING, FAILED, or ALL (default: ALL)
     * @param from start date (inclusive), optional
     * @param to end date (inclusive), optional
     * @param page page number (0-indexed, default: 0)
     * @param size page size (default: 10)
     * @return paginated transaction list
     */
    @GetMapping
    public ResponseEntity<Page<TransactionListResponse>> getTransactionHistory(
            @RequestParam(name = "type", defaultValue = "ALL") String type,
            @RequestParam(name = "status", defaultValue = "ALL") String status,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionListResponse> result = transactionQueryService.getTransactionHistory(
                type, status, from, to, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * T-053: Gets the latest 10 transactions (mini-statement).
     *
     * @return latest 10 transactions
     */
    @GetMapping("/mini")
    public ResponseEntity<Page<TransactionListResponse>> getMiniStatement() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<TransactionListResponse> result = transactionQueryService.getMiniStatement(pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * T-054: Gets details for a specific transaction.
     * Only accessible to sender or receiver.
     *
     * @param id transaction ID
     * @return transaction details or HTTP 404/403
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransactionDetail(@PathVariable UUID id) {
        TransferResponse response = transactionQueryService.getTransactionDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * T-055: Gets personal statistics (total sent, total received, monthly count).
     *
     * @return statistics response
     */
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics() {
        StatisticsResponse response = transactionQueryService.getStatistics();
        return ResponseEntity.ok(response);
    }
}

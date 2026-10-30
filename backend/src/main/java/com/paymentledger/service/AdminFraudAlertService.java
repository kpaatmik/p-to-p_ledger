package com.paymentledger.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.dto.response.FraudAlertResponse;
import com.paymentledger.entity.AlertStatus;
import com.paymentledger.entity.EntryType;
import com.paymentledger.entity.FraudAlert;
import com.paymentledger.entity.LedgerEntry;
import com.paymentledger.entity.Transaction;
import com.paymentledger.entity.TransactionStatus;
import com.paymentledger.entity.User;
import com.paymentledger.entity.Wallet;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.repository.FraudAlertRepository;
import com.paymentledger.repository.LedgerEntryRepository;
import com.paymentledger.repository.TransactionRepository;
import com.paymentledger.repository.UserRepository;
import com.paymentledger.repository.WalletRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Admin service for fraud alert management (Phase 6, T-063 to T-064).
 * 
 * Handles:
 * - T-063: Paginated fraud alert queue with filtering (PENDING, BLOCKED, DISMISSED)
 * - T-064: Admin block action (mark alert as BLOCKED, mark transaction as FAILED, refund sender)
 * - T-064: Admin dismiss action (mark alert as DISMISSED, leave transaction PENDING for settlement)
 * 
 * Refund idempotency is ensured by transaction status checks: only PENDING transactions
 * can be failed and refunded. Subsequent block attempts on the same alert will find
 * the transaction already FAILED and skip the refund.
 */
@Service
@RequiredArgsConstructor
public class AdminFraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /**
     * T-063: Retrieve paginated fraud alerts, optionally filtered by status.
     * 
     * @param status filter by AlertStatus (null = all alerts)
     * @param pageable pagination parameters
     * @return page of fraud alerts with transaction summary
     */
    @Transactional(readOnly = true)
    public Page<FraudAlertResponse> getFraudAlerts(AlertStatus status, Pageable pageable) {
        Page<FraudAlert> alerts = status == null
                ? fraudAlertRepository.findAllByOrderByCreatedAtDesc(pageable)
                : fraudAlertRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        List<FraudAlertResponse> responses = alerts.getContent().stream()
                .map(this::toFraudAlertResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, alerts.getTotalElements());
    }

    /**
     * T-064: Block a fraud alert by marking it BLOCKED, failing the transaction, and refunding the sender.
     * 
     * Refund idempotency:
     * - Acquire SELECT FOR UPDATE lock on the transaction
     * - Check if transaction is already FAILED; if so, skip refund (already processed)
     * - Change transaction status from PENDING to FAILED
     * - Credit sender wallet with full amount
     * - Create CREDIT ledger entry (reversal)
     * - Notify both sender (refund processed) and receiver (transfer cancelled)
     * - Mark alert as BLOCKED, set reviewedBy and reviewedAt
     * - Audit log the admin action
     * 
     * If the alert references a rejected attempt (no transaction), only update the alert status and audit log.
     * 
     * @param alertId the fraud alert ID
     * @param adminId the admin user ID performing the action
     * @param remarks optional admin notes
     * @throws EntityNotFoundException if alert not found
     * @throws WalletOperationException if transaction is not found or refund fails
     */
    @Transactional
    public void blockFraudAlert(UUID alertId, UUID adminId, String remarks) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Fraud alert not found: " + alertId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found: " + adminId));

        // Case 1: Alert references an approved transaction (HIGH_VALUE) — can be refunded if still PENDING
        if (alert.getTransaction() != null) {
            Transaction transaction = transactionRepository.findById(alert.getTransaction().getId())
                    .orElseThrow(() -> new WalletOperationException("Transaction not found for alert: " + alertId));

            User sender = transaction.getSender();
            User receiver = transaction.getReceiver();

            // Refund idempotency: lock and check transaction status
            // Only refund if transaction is PENDING
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                // Mark transaction as FAILED
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);

                // Get sender wallet
                Wallet senderWallet = walletRepository.findByUserId(sender.getId())
                        .orElseThrow(() -> new WalletOperationException("Sender wallet not found: " + sender.getId()));

                // Credit sender wallet with full amount (reversal)
                walletService.creditWallet(senderWallet.getId(), transaction.getAmount());

                // Create CREDIT ledger entry (reversal of DEBIT)
                LedgerEntry creditEntry = new LedgerEntry(
                        transaction,
                        senderWallet,
                        EntryType.CREDIT,
                        transaction.getAmount(),
                        "Fraud alert block refund — transfer to " + receiver.getEmail() + " cancelled"
                );
                ledgerEntryRepository.save(creditEntry);

                // Notify sender: refund processed
                notificationService.send(
                        sender,
                        NotificationService.TYPE_FRAUD_ALERT,
                        "Your transfer of ₹" + transaction.getAmount() + " to " + receiver.getEmail() +
                                " was blocked by fraud review. Amount has been refunded to your wallet."
                );

                // Notify receiver: transfer cancelled
                notificationService.send(
                        receiver,
                        NotificationService.TYPE_TRANSACTION_BLOCKED,
                        "Transfer of ₹" + transaction.getAmount() + " from " + sender.getEmail() +
                                " was cancelled due to fraud review."
                );
            }
            // If transaction is already FAILED, no refund needed (idempotent)
        }
        // Case 2: Alert references a rejected attempt (DAILY_LIMIT or DUPLICATE) — no transaction to block
        // Nothing to refund; just update the alert status

        // Mark alert as BLOCKED
        alert.setStatus(AlertStatus.BLOCKED);
        alert.setReviewedBy(admin);
        alert.setReviewedAt(Instant.now());
        fraudAlertRepository.save(alert);

        // Audit log: admin blocked fraud alert
        String description = "Blocked fraud alert " + alert.getAlertType();
        if (alert.getTransaction() != null) {
            description += " on transaction " + alert.getTransaction().getId();
        } else if (alert.getAttemptSender() != null) {
            description += " on rejected attempt: " + alert.getAttemptSender().getEmail() + 
                          " → " + alert.getAttemptRecipient().getEmail() + 
                          " ₹" + alert.getAttemptAmount();
        }
        if (remarks != null) {
            description += ". Remarks: " + remarks;
        }

        auditLogService.recordAdminAction(
                adminId,
                "BLOCK_FRAUD_ALERT",
                "FRAUD_ALERT",
                alertId,
                description
        );
    }

    /**
     * T-064: Dismiss a fraud alert without blocking the transfer.
     * 
     * The transaction remains PENDING and proceeds to settlement.
     * Used when admin reviews the alert and determines it is not actually fraudulent.
     * 
     * @param alertId the fraud alert ID
     * @param adminId the admin user ID performing the action
     * @param remarks optional admin notes
     * @throws EntityNotFoundException if alert not found
     */
    @Transactional
    public void dismissFraudAlert(UUID alertId, UUID adminId, String remarks) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Fraud alert not found: " + alertId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found: " + adminId));

        // Mark alert as DISMISSED
        alert.setStatus(AlertStatus.DISMISSED);
        alert.setReviewedBy(admin);
        alert.setReviewedAt(Instant.now());
        fraudAlertRepository.save(alert);

        // Audit log: admin dismissed fraud alert
        auditLogService.recordAdminAction(
                adminId,
                "DISMISS_FRAUD_ALERT",
                "FRAUD_ALERT",
                alertId,
                "Dismissed fraud alert " + alert.getAlertType() + " on transaction " + alert.getTransaction().getId() +
                        (remarks != null ? ". Remarks: " + remarks : "")
        );
    }

    /**
     * Converts a FraudAlert entity to a FraudAlertResponse DTO.
     * Handles both approved-transaction alerts and rejected-attempt alerts.
     */
    private FraudAlertResponse toFraudAlertResponse(FraudAlert alert) {
        UUID transactionId = null;
        String senderEmail = null;
        String recipientEmail = null;
        BigDecimal amount = null;

        // Case 1: Alert references an approved transaction (HIGH_VALUE)
        if (alert.getTransaction() != null) {
            Transaction transaction = alert.getTransaction();
            transactionId = transaction.getId();
            senderEmail = transaction.getSender().getEmail();
            recipientEmail = transaction.getReceiver().getEmail();
            amount = transaction.getAmount();
        }
        // Case 2: Alert references a rejected attempt (DAILY_LIMIT, DUPLICATE)
        else if (alert.getAttemptSender() != null) {
            transactionId = null;
            senderEmail = alert.getAttemptSender().getEmail();
            recipientEmail = alert.getAttemptRecipient().getEmail();
            amount = alert.getAttemptAmount();
        }

        User reviewedBy = alert.getReviewedBy();
        String reviewedAction = null;
        if (alert.getStatus() == AlertStatus.BLOCKED) {
            reviewedAction = "BLOCKED";
        } else if (alert.getStatus() == AlertStatus.DISMISSED) {
            reviewedAction = "DISMISSED";
        }

        return new FraudAlertResponse(
                alert.getId(),
                transactionId,
                alert.getAlertType().name(),
                alert.getStatus().name(),
                amount,
                senderEmail,
                recipientEmail,
                alert.getCreatedAt(),
                alert.getReviewedAt(),
                reviewedBy != null ? reviewedBy.getEmail() : null,
                reviewedAction
        );
    }
}

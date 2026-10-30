package com.paymentledger.service;

import com.paymentledger.entity.*;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.repository.LedgerEntryRepository;
import com.paymentledger.repository.TransactionRepository;
import com.paymentledger.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Settlement service for P2P transfer settlement (Phase 8, T-091, T-092, T-093).
 * Each transaction is settled independently in its own transaction.
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final NotificationService notificationService;

    /**
     * T-091, T-092: Settle a single P2P transaction.
     *
     * Use REQUIRES_NEW so one transaction's failure does not affect others in the job batch.
     *
     * Process:
     * 1. Lock transaction row
     * 2. Return safely if not PENDING
     * 3. Lock receiver wallet
     * 4. Verify transaction and wallet eligibility
     * 5. Credit receiver wallet
     * 6. Create CREDIT ledger entry
     * 7. Mark transaction SUCCESS
     * 8. Set settled_at
     * 9. Send notifications
     *
     * @param transactionId the transaction to settle
     * @return true if settled, false if already settled/failed/declined
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean settleTransaction(UUID transactionId) {
        // Step 1: Lock transaction row
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new WalletOperationException("Transaction not found for settlement"));

        // Step 2: Return safely if not PENDING (already settled/failed/declined)
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            return false;
        }

        // Get receiver
        User receiver = transaction.getReceiver();
        if (receiver == null) {
            throw new WalletOperationException("Receiver not found for transaction");
        }

        // Step 3: Lock receiver wallet
        Wallet receiverWallet = walletRepository.findByUserId(receiver.getId())
                .orElseThrow(() -> new WalletOperationException("Receiver wallet not found"));

        // Step 4: Verify transaction and wallet eligibility
        // Verify receiver wallet is ACTIVE
        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            // T-093: Wallet inactive — decline and refund sender
            return handleSettlementDecline(transaction, "Receiver wallet is not active");
        }

        // Verify transaction amount is positive
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return handleSettlementFailed(transaction, "Invalid transaction amount");
        }

        try {
            // Step 5: Credit receiver wallet
            walletRepository.save(receiverWallet);
            receiverWallet.setTotalBalance(receiverWallet.getTotalBalance().add(transaction.getAmount()));
            walletRepository.save(receiverWallet);

            // Step 6: Create CREDIT ledger entry
            LedgerEntry creditEntry = new LedgerEntry(
                    transaction,
                    receiverWallet,
                    EntryType.CREDIT,
                    transaction.getAmount(),
                    "P2P transfer from " + transaction.getSender().getEmail()
            );
            ledgerEntryRepository.save(creditEntry);

            // Step 7: Mark transaction SUCCESS
            transaction.setStatus(TransactionStatus.SUCCESS);

            // Step 8: Set settled_at
            transaction.setSettledAt(Instant.now());
            transactionRepository.save(transaction);

            // Step 9: Send notifications
            notificationService.send(
                    transaction.getSender(),
                    NotificationService.TYPE_SETTLEMENT_SUCCESS,
                    String.format("Your transfer of ₹%s to %s has been settled successfully.",
                            transaction.getAmount(), receiver.getEmail())
            );

            notificationService.send(
                    receiver,
                    NotificationService.TYPE_SETTLEMENT_SUCCESS,
                    String.format("You received ₹%s from %s.",
                            transaction.getAmount(), transaction.getSender().getEmail())
            );

            return true;

        } catch (Exception ex) {
            // Capture exception and mark as failed
            return handleSettlementFailed(transaction, "Settlement error: " + ex.getMessage());
        }
    }

    /**
     * T-093: Handle settlement decline (receiver wallet inactive/frozen).
     * Refund sender exactly once in a separate transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private boolean handleSettlementDecline(Transaction transaction, String reason) {
        // Mark transaction DECLINED
        transaction.setStatus(TransactionStatus.DECLINED);
        transactionRepository.save(transaction);

        // Refund sender
        try {
            User sender = transaction.getSender();
            Wallet senderWallet = walletRepository.findByUserId(sender.getId())
                    .orElseThrow(() -> new WalletOperationException("Sender wallet not found for refund"));

            // Credit sender wallet (refund)
            senderWallet.setTotalBalance(senderWallet.getTotalBalance().add(transaction.getAmount()));
            walletRepository.save(senderWallet);

            // Create CREDIT refund ledger entry
            LedgerEntry refundEntry = new LedgerEntry(
                    transaction,
                    senderWallet,
                    EntryType.CREDIT,
                    transaction.getAmount(),
                    "Refund: " + reason
            );
            ledgerEntryRepository.save(refundEntry);

            // Notify sender
            notificationService.send(
                    sender,
                    NotificationService.TYPE_SETTLEMENT_DECLINED,
                    String.format("Your transfer of ₹%s could not be delivered: %s. Refunded to your account.",
                            transaction.getAmount(), reason)
            );

        } catch (Exception ex) {
            // Log but don't throw — settlement is marked declined
        }

        return false;
    }

    /**
     * T-093: Handle permanent settlement failure (non-recoverable).
     * Mark transaction FAILED and refund sender exactly once.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private boolean handleSettlementFailed(Transaction transaction, String reason) {
        // Mark transaction FAILED
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);

        // Refund sender
        try {
            User sender = transaction.getSender();
            Wallet senderWallet = walletRepository.findByUserId(sender.getId())
                    .orElseThrow(() -> new WalletOperationException("Sender wallet not found for refund"));

            // Credit sender wallet (refund)
            senderWallet.setTotalBalance(senderWallet.getTotalBalance().add(transaction.getAmount()));
            walletRepository.save(senderWallet);

            // Create CREDIT refund ledger entry
            LedgerEntry refundEntry = new LedgerEntry(
                    transaction,
                    senderWallet,
                    EntryType.CREDIT,
                    transaction.getAmount(),
                    "Refund: " + reason
            );
            ledgerEntryRepository.save(refundEntry);

            // Notify sender
            notificationService.send(
                    sender,
                    NotificationService.TYPE_SETTLEMENT_FAILED,
                    String.format("Your transfer of ₹%s failed: %s. Refunded to your account.",
                            transaction.getAmount(), reason)
            );

        } catch (Exception ex) {
            // Log but don't throw — settlement is marked failed
        }

        return false;
    }
}

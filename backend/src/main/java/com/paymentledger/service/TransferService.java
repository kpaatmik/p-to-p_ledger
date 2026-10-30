package com.paymentledger.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.dto.request.TransferRequest;
import com.paymentledger.dto.response.TransferResponse;
import com.paymentledger.entity.EntryType;
import com.paymentledger.entity.LedgerEntry;
import com.paymentledger.entity.Transaction;
import com.paymentledger.entity.TransactionStatus;
import com.paymentledger.entity.TransactionType;
import com.paymentledger.entity.User;
import com.paymentledger.entity.Wallet;
import com.paymentledger.entity.WalletStatus;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.repository.LedgerEntryRepository;
import com.paymentledger.repository.NotificationRepository;
import com.paymentledger.repository.TransactionRepository;
import com.paymentledger.repository.UserRepository;
import com.paymentledger.util.BusinessDayCalculator;

/**
 * Transfer service for P2P money transfers (Phase 5, T-051 to T-055).
 * Coordinates wallet operations, transaction creation, ledger entries, and notifications.
 * Integrates fraud detection (Phase 6, T-060 to T-062).
 * All financial operations are atomic and transactional.
 */
@Service
public class TransferService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final NotificationRepository notificationRepository;
    private final WalletService walletService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final FraudService fraudService;

    public TransferService(UserRepository userRepository,
                           TransactionRepository transactionRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           NotificationRepository notificationRepository,
                           WalletService walletService,
                           UserService userService,
                           NotificationService notificationService,
                           PasswordEncoder passwordEncoder,
                           FraudService fraudService) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.notificationRepository = notificationRepository;
        this.walletService = walletService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.fraudService = fraudService;
    }

    /**
     * T-051 + T-052: Initiates a P2P transfer atomically.
     * Integrates fraud detection (T-060, T-061, T-062).
     * 
     * Validations:
     * - Sender != Recipient
     * - Amount positive
     * - PIN correct (BCrypt verification)
     * - Sender wallet ACTIVE
     * - Sufficient available balance
     * - T-060: Daily limit not exceeded (BLOCKS if exceeded)
     * - T-061: No duplicate within 2 minutes (BLOCKS if found)
     * - T-062: High-value threshold (creates alert, does NOT block)
     * 
     * Idempotency (T-052):
     * - Check request_id before processing
     * - If exists, return original transaction
     * - DB UNIQUE constraint prevents race conditions
     * 
     * Atomic operations:
     * - Lock sender wallet (pessimistic)
     * - T-060: Check daily limit (throws if exceeded + creates rejection alert)
     * - T-061: Check duplicate (throws if found + creates rejection alert)
     * - Debit sender wallet
     * - Create Transaction record (PENDING)
     * - Create sender DEBIT ledger entry
     * - T-062: Create high-value alert if threshold exceeded (same transaction)
     * - Send notification
     * 
     * @param request contains recipientId, amount, transactionPin, requestId
     * @return transfer response with transaction details
     * @throws WalletOperationException for validation failures or fraud rejections
     */
    @Transactional
    public TransferResponse initiateTransfer(TransferRequest request) {
        // T-052: Check idempotency — if request_id exists, return original transaction
        Optional<Transaction> existingTransaction = transactionRepository.findByRequestId(request.requestId());
        if (existingTransaction.isPresent()) {
            Transaction existing = existingTransaction.get();
            return buildTransferResponse(existing);
        }

        // Get authenticated sender
        User sender = userService.getAuthenticatedUser();

        // Get recipient
        User recipient = userRepository.findById(request.recipientId())
                .orElseThrow(() -> new WalletOperationException("Recipient not found"));

        // Validation: sender != recipient
        if (sender.getId().equals(recipient.getId())) {
            throw new WalletOperationException("Cannot transfer to yourself");
        }

        // Validation: amount positive
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletOperationException("Transfer amount must be positive");
        }

        // Validation: PIN correct
        if (!passwordEncoder.matches(request.transactionPin(), sender.getPinHash())) {
            throw new WalletOperationException("Incorrect transaction PIN");
        }

        // Get sender and recipient wallets (with lock on sender)
        Wallet senderWallet = walletService.getWalletWithLockByUserId(sender.getId());
        Wallet recipientWallet = walletService.getWalletByUserId(recipient.getId());

        // Validation: sender wallet ACTIVE
        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletOperationException("Sender wallet is not active");
        }

        // Validation: recipient wallet ACTIVE
        if (recipientWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletOperationException("Recipient wallet is not active");
        }

        // Validation: sufficient available balance
        BigDecimal available = senderWallet.getTotalBalance().subtract(senderWallet.getReservedBalance());
        if (available.compareTo(request.amount()) < 0) {
            throw new WalletOperationException("Insufficient available balance");
        }

        // ── Phase 6 Fraud Detection (T-060, T-061) ────────────────────────
        // Sender wallet is locked; perform fraud checks BEFORE debit
        // Both checks throw exceptions if they fail (reject the transfer)
        
        // T-060: Daily limit check (BLOCKS if exceeded, creates rejection alert)
        fraudService.checkDailyLimitExceeded(sender.getId(), sender, recipient, request.amount());
        
        // T-061: Duplicate detection (BLOCKS if found, creates rejection alert)
        fraudService.checkDuplicate(sender.getId(), recipient.getId(), request.amount(), sender, recipient);

        // Calculate settlement date: T+3 business days
        LocalDate settlementDate = BusinessDayCalculator.getSettlementDateUtc();

        // Create Transaction record (status = PENDING)
        Transaction transaction = new Transaction(
                request.requestId(),
                sender,
                recipient,
                request.amount(),
                null,  // no remark for P2P
                TransactionType.P2P_TRANSFER,
                settlementDate
        );
        transaction.setStatus(TransactionStatus.PENDING);
        transactionRepository.save(transaction);

        // Debit sender wallet atomically (this reacquires the lock internally)
        walletService.debitWallet(senderWallet.getId(), request.amount());

        // Create sender DEBIT ledger entry
        ledgerEntryRepository.save(new LedgerEntry(
                transaction,
                senderWallet,
                EntryType.DEBIT,
                request.amount(),
                "P2P transfer to " + recipient.getEmail()
        ));

        // ── Phase 6 Fraud Detection (T-062) ────────────────────────────────
        // T-062: High-value alert (NON-BLOCKING)
        // Created in SAME transaction as the approved PENDING transaction
        fraudService.createHighValueAlert(transaction);

        // Create transfer initiation notification for sender
        notificationService.send(
                sender,
                NotificationService.TYPE_TRANSFER_INITIATED,
                "Transfer of ₹" + request.amount() + " to " + recipient.getEmail() + " is pending settlement."
        );

        return buildTransferResponse(transaction);
    }

    /**
     * Builds a TransferResponse from a Transaction entity.
     * Includes calculated days until settlement for PENDING transactions.
     */
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

package com.paymentledger.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.entity.AlertStatus;
import com.paymentledger.entity.AlertType;
import com.paymentledger.entity.CheckPleaseReservation;
import com.paymentledger.entity.FraudAlert;
import com.paymentledger.entity.Merchant;
import com.paymentledger.entity.Transaction;
import com.paymentledger.entity.User;
import com.paymentledger.entity.Wallet;
import com.paymentledger.entity.WalletStatus;
import com.paymentledger.exception.InvalidPinException;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.util.PinHashingUtil;
import com.paymentledger.repository.FraudAlertRepository;
import com.paymentledger.repository.TransactionRepository;
import com.paymentledger.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Fraud detection service for Phase 6 (T-060 to T-062).
 * 
 * Implements three fraud checks:
 * - T-060: Daily transfer limit (₹50,000 per day) — BLOCKS transfer if exceeded
 * - T-061: Duplicate transaction detection (same sender/receiver/amount within 2 minutes) — BLOCKS transfer
 * - T-062: High-value alert (amount > ₹8,000) — ALLOWS transfer, creates alert for review
 * 
 * Transaction propagation strategy:
 * - T-060 and T-061 rejection alerts: created in REQUIRES_NEW so they persist even if transfer fails
 * - T-062 HIGH_VALUE alert: created in SAME transaction as the approved PENDING transaction
 * 
 * Rejection flow (T-060, T-061):
 * - Check passes: proceed normally
 * - Check fails: create rejection alert with attempt metadata (sender, recipient, amount), throw exception
 * - Wallet never locked or debited; no Transaction record created
 * 
 * Approval flow (T-062):
 * - Transaction created and PENDING status set
 * - If amount > ₹8,000: create HIGH_VALUE alert in same transaction
 * - Both commit together
 */
@Service
@RequiredArgsConstructor
public class FraudService {

    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PinHashingUtil pinHashingUtil;
    
    @Value("${fraud.daily-limit:50000.0000}")
    private BigDecimal dailyLimitAmount;
    
    @Value("${fraud.high-value-threshold:8000.0000}")
    private BigDecimal highValueThreshold;

    /**
     * T-060: Checks whether a transfer would exceed the daily limit (₹50,000 per sender per day).
     * 
     * BLOCKS the transfer if limit is exceeded (existing total + amount > ₹50,000).
     * Exactly ₹50,000 is allowed.
     * 
     * Caller MUST hold a pessimistic lock on sender's wallet before calling this method.
     * 
     * @param senderId the sender's user ID (must have wallet locked)
     * @param amount the requested transfer amount
     * @throws WalletOperationException if daily limit exceeded (includes alert creation)
     */
    @Transactional
    public void checkDailyLimitExceeded(UUID senderId, User sender, User recipient, BigDecimal amount) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        
        // Sum all non-failed/declined P2P transfers for this sender today
        BigDecimal todayTotal = transactionRepository.sumSentAmountSince(senderId, startOfDay);
        if (todayTotal == null) {
            todayTotal = BigDecimal.ZERO;
        }
        
        BigDecimal projectedTotal = todayTotal.add(amount);
        
        // Check: projected total must NOT exceed limit
        // Exactly ₹50,000 is allowed
        if (projectedTotal.compareTo(dailyLimitAmount) > 0) {
            // Create rejection alert in independent transaction
            createDailyLimitRejectionAlert(sender, recipient, amount);
            
            // Reject the transfer
            throw new WalletOperationException(
                "Daily transfer limit exceeded. You have sent ₹" + todayTotal + 
                " today; this transfer would exceed the ₹" + dailyLimitAmount + " limit."
            );
        }
    }

    /**
     * T-061: Checks for duplicate transactions (same sender, receiver, amount within 2 minutes).
     * 
     * BLOCKS the transfer if a matching PENDING or SUCCESS transaction exists within the last 2 minutes.
     * 
     * @param senderId the sender's user ID
     * @param receiverId the receiver's user ID
     * @param amount the transfer amount
     * @param sender User entity for alert creation
     * @param recipient User entity for alert creation
     * @throws WalletOperationException if duplicate detected (includes alert creation)
     */
    @Transactional
    public void checkDuplicate(UUID senderId, UUID receiverId, BigDecimal amount, User sender, User recipient) {
        Instant twoMinutesAgo = Instant.now().minusSeconds(120);
        
        long recentDuplicateCount = transactionRepository.countRecentDuplicates(
                senderId, receiverId, amount, twoMinutesAgo);
        
        if (recentDuplicateCount > 0) {
            // Create rejection alert in independent transaction
            createDuplicateRejectionAlert(sender, recipient, amount);
            
            // Reject the transfer
            throw new WalletOperationException(
                "Duplicate transfer detected. An identical transfer to this recipient was made within the last 2 minutes. " +
                "Please try again after 2 minutes."
            );
        }
    }

    /**
     * T-062: Creates a HIGH_VALUE fraud alert for transfers > ₹8,000.
     * 
     * Does NOT block the transfer. The alert is created for admin review.
     * Called AFTER the transaction is persisted, but in the SAME transaction (not REQUIRES_NEW).
     * This ensures the alert and transaction commit together.
     * 
     * @param transaction the transaction entity (must not be null)
     */
    @Transactional
    public void createHighValueAlert(Transaction transaction) {
        // Strict comparison: > ₹8,000 (not >=)
        if (transaction.getAmount().compareTo(highValueThreshold) > 0) {
            FraudAlert alert = new FraudAlert(transaction, AlertType.HIGH_VALUE);
            alert.setStatus(AlertStatus.PENDING);
            fraudAlertRepository.save(alert);
        }
    }

    /**
     * T-060 rejection alert: create in independent transaction so it persists on transfer rejection.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createDailyLimitRejectionAlert(User sender, User recipient, BigDecimal amount) {
        FraudAlert alert = new FraudAlert(sender, recipient, amount, AlertType.DAILY_LIMIT);
        alert.setStatus(AlertStatus.PENDING);
        fraudAlertRepository.save(alert);
        
        // Send notification to sender about the rejection
        notificationService.send(
            sender,
            NotificationService.TYPE_FRAUD_ALERT,
            "Your transfer of ₹" + amount + " was blocked: daily limit of ₹" + dailyLimitAmount + " exceeded."
        );
    }

    /**
     * T-061 rejection alert: create in independent transaction so it persists on transfer rejection.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createDuplicateRejectionAlert(User sender, User recipient, BigDecimal amount) {
        FraudAlert alert = new FraudAlert(sender, recipient, amount, AlertType.DUPLICATE);
        alert.setStatus(AlertStatus.PENDING);
        fraudAlertRepository.save(alert);
        
        // Send notification to sender about the rejection
        notificationService.send(
            sender,
            NotificationService.TYPE_FRAUD_ALERT,
            "Your transfer of ₹" + amount + " was blocked: duplicate transfer detected within 2 minutes."
        );
    }

    /**
     * T-078: Merchant payment fraud check.
     *
     * Applies MANDATORY controls for merchant payments:
     * 1. Valid JWT — guaranteed by controller @PreAuthorize
     * 2. Authenticated user owns the reservation — enforced by checkPleaseService
     * 3. Correct 4-digit transaction PIN — verified here
     * 4. Wallet is ACTIVE — verified by checkPleaseService
     * 5. Merchant is ACTIVE — verified by checkPleaseService
     * 6. Merchant matches reservation — verified by checkPleaseService
     * 7. Account is not locked — checked here
     * 8. Idempotency via request_id — enforced by repository unique constraint
     * 9. Reservation is ACTIVE and valid — verified by checkPleaseService
     *
     * A valid ACTIVE Check Please reservation acts as a positive risk signal.
     * No additional merchant-specific fraud block is required for MVP.
     *
     * Merchant payments do NOT trigger:
     * - ₹50,000 P2P daily transfer limit (P2P-specific, does not apply)
     * - Same recipient/amount duplicate-transfer rule (P2P-specific, does not apply)
     * - ₹8,000 high-value alert (P2P-specific, does not apply)
     *
     * Wrong PIN throws InvalidPinException (maps to HTTP 401).
     * Other violations throw WalletOperationException.
     *
     * @param userId the authenticated user ID
     * @param user the authenticated user entity
     * @param reservation the Check Please reservation
     * @param merchant the merchant
     * @param wallet the user's wallet
     * @param amount the payment amount
     * @param transactionPin the user-supplied 4-digit PIN
     * @throws InvalidPinException if PIN is incorrect (HTTP 401)
     * @throws WalletOperationException if any mandatory control is violated
     */
    @Transactional
    public void checkMerchantPayment(UUID userId, User user, CheckPleaseReservation reservation,
                                      Merchant merchant, Wallet wallet, BigDecimal amount,
                                      String transactionPin) {

        // Control 1: Valid JWT — already guaranteed by @PreAuthorize in controller

        // Control 2: Authenticated user owns the reservation
        if (!reservation.getUser().getId().equals(userId)) {
            throw new WalletOperationException("User does not own the reservation");
        }

        // Control 3: Correct transaction PIN
        if (!pinHashingUtil.verifyPin(transactionPin, user.getPinHash())) {
            throw new InvalidPinException("Incorrect transaction PIN");
        }

        // Control 4: Wallet is ACTIVE
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletOperationException("Wallet is not active");
        }

        // Control 5 & 6: Merchant is ACTIVE and matches reservation
        if (merchant.getStatus() != com.paymentledger.entity.MerchantStatus.ACTIVE) {
            throw new WalletOperationException("Merchant is not active");
        }
        if (!merchant.getId().equals(reservation.getMerchant().getId())) {
            throw new WalletOperationException("Merchant does not match reservation");
        }

        // Control 7: Account is not locked
        if (user.isAccountLocked()) {
            throw new WalletOperationException("Account is locked");
        }

        // Control 8: Idempotency — handled at repository level (UNIQUE constraint on request_id)

        // Control 9: Reservation is ACTIVE and valid — validated by checkPleaseService

        // Policy: A valid ACTIVE Check Please reservation is a positive risk signal.
        // No additional merchant-specific fraud block required for MVP.
    }
}

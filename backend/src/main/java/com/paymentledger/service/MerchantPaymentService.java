package com.paymentledger.service;

import com.paymentledger.dto.request.MerchantPaymentRequest;
import com.paymentledger.dto.response.MerchantPaymentResponse;
import com.paymentledger.entity.*;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.repository.*;
import com.paymentledger.util.PinHashingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Merchant payment service for T-075 through T-076.
 * Manages merchant payment initiation and capture with idempotency and fraud checks.
 */
@Service
@RequiredArgsConstructor
public class MerchantPaymentService {

    private final MerchantPaymentRepository merchantPaymentRepository;
    private final CheckPleaseReservationRepository reservationRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final MerchantService merchantService;
    private final CheckPleaseService checkPleaseService;
    private final FraudService fraudService;
    private final NotificationService notificationService;
    private final PinHashingUtil pinHashingUtil;

    /**
     * T-075: Initiate a merchant payment.
     *
     * Validates:
     * - Authenticated user owns the reservation
     * - Merchant matches the reservation
     * - Reservation is ACTIVE and not expired
     * - Payment amount is positive and ≤ remaining_amount
     * - Wallet is ACTIVE and not frozen
     * - Account is not locked
     * - Transaction PIN is correct
     * - Mandatory security checks pass
     *
     * Use request_id idempotency:
     * - Same user and same request_id must return the original result
     * - Do not create a second payment or debit twice
     * - Enforce idempotency at database level
     * - Do not expose another user's payment through request_id lookup
     *
     * Creates MerchantPayment with PENDING status before capture.
     *
     * Wrong PIN returns HTTP 401. Other validation errors use existing conventions.
     *
     * @param request the payment request with idempotency key, reservation ID, amount, PIN
     * @return payment response with payment details and updated wallet balances
     * @throws WalletOperationException on validation failure
     * @throws InvalidPinException on wrong PIN (maps to HTTP 401)
     */
    @Transactional
    public MerchantPaymentResponse initiatePayment(MerchantPaymentRequest request) {
        UUID userId = getAuthenticatedUserId();

        // Idempotency: check if request_id already processed (same user context)
        var existing = merchantPaymentRepository.findByRequestId(request.requestId());
        if (existing.isPresent()) {
            MerchantPayment existingPayment = existing.get();
            // Verify ownership — do not expose another user's payment
            if (!existingPayment.getUser().getId().equals(userId)) {
                throw new WalletOperationException("Invalid request");
            }
            // Return original result based on current status
            return toMerchantPaymentResponse(existingPayment);
        }

        // Load user, wallet, and reservation with locks
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WalletOperationException("User not found"));

        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));

        CheckPleaseReservation reservation = reservationRepository.findByIdWithLock(request.reservationId())
                .orElseThrow(() -> new WalletOperationException("Reservation not found"));

        Merchant merchant = merchantService.getMerchantByIdUnchecked(reservation.getMerchant().getId());

        // Run all security and fraud checks before capture
        try {
            // T-078: Merchant-specific fraud checks
            fraudService.checkMerchantPayment(
                    userId,
                    user,
                    reservation,
                    merchant,
                    wallet,
                    request.amount(),
                    request.transactionPin()
            );

            // Validate reservation
            checkPleaseService.validateReservation(
                    request.reservationId(),
                    userId,
                    merchant.getId(),
                    request.amount()
            );

        } catch (com.paymentledger.exception.InvalidPinException ex) {
            // Wrong PIN → HTTP 401
            throw ex;
        } catch (Exception ex) {
            // Other validation errors
            throw new WalletOperationException(ex.getMessage());
        }

        // Create MerchantPayment record with PENDING status
        MerchantPayment payment = new MerchantPayment(
                request.requestId(),
                reservation,
                user,
                merchant,
                wallet,
                request.amount()
        );
        payment.setStatus(PaymentStatus.PENDING);
        payment = merchantPaymentRepository.save(payment);

        // Proceed to capture in same transaction
        try {
            capturePayment(payment, wallet, reservation);
            return toMerchantPaymentResponse(payment);
        } catch (Exception ex) {
            // Capture failed — transaction rolls back, payment stays PENDING
            // Failure handling in T-077 caller
            throw ex;
        }
    }

    /**
     * T-076: Capture a merchant payment.
     *
     * Use one atomic transaction and pessimistic write locks on reservation and wallet.
     *
     * Capture must:
     * 1. Lock and revalidate the reservation
     * 2. Lock the wallet
     * 3. Verify payment and reservation are still eligible
     * 4. Verify available and reserved balances are consistent
     * 5. Debit wallet total_balance by payment amount
     * 6. Create a DEBIT LedgerEntry for the captured payment
     * 7. Reduce reservation remaining_amount by payment_amount
     * 8. Release reservation's entire original remaining_amount from reserved_balance
     * 9. Set reservation status to CONSUMED
     * 10. Set payment status to CAPTURED
     * 11. Send payment success notification
     *
     * Important balance calculation:
     * Before capture:
     * - total_balance = T
     * - reserved_balance = R
     * - reservation remaining_amount = R (or its allocated portion)
     * - payment_amount = P
     *
     * After successful capture:
     * - total_balance = T - P
     * - reserved_balance = reserved_balance - R
     * - reservation remaining_amount = R - P
     * - available_balance increases by R - P
     *
     * The capture must be retry-safe and never capture twice.
     *
     * @param payment the payment to capture
     * @param wallet the wallet (locked)
     * @param reservation the reservation (locked)
     * @throws WalletOperationException on capture failure
     */
    @Transactional
    public void capturePayment(MerchantPayment payment, Wallet wallet,
                               CheckPleaseReservation reservation) {
        // Relock and revalidate reservation (in case conditions changed)
        CheckPleaseReservation locked = reservationRepository.findByIdWithLock(payment.getReservation().getId())
                .orElseThrow(() -> new WalletOperationException("Reservation not found during capture"));

        // Relock wallet
        Wallet lockedWallet = walletRepository.findByIdWithLock(payment.getWallet().getId())
                .orElseThrow(() -> new WalletOperationException("Wallet not found during capture"));

        // Verify reservation is still ACTIVE
        if (locked.getStatus() != ReservationStatus.ACTIVE) {
            throw new WalletOperationException("Reservation is not active for payment");
        }

        // Verify not expired
        if (!locked.getExpiresAt().isAfter(Instant.now())) {
            throw new WalletOperationException("Reservation has expired");
        }

        // Verify payment amount is still valid
        if (payment.getAmount().compareTo(locked.getRemainingAmount()) > 0) {
            throw new WalletOperationException(
                    String.format("Payment amount exceeds remaining reservation amount. " +
                            "Remaining: ₹%s, Payment: ₹%s",
                            locked.getRemainingAmount(), payment.getAmount())
            );
        }

        // Verify payment is still PENDING
        if (payment.getStatus() != PaymentStatus.PENDING) {
            // Already captured or failed — prevent double-capture
            throw new WalletOperationException("Payment is already " + payment.getStatus());
        }

        // Verify wallet still has funds
        BigDecimal totalBalance = lockedWallet.getTotalBalance();
        BigDecimal reservedBalance = lockedWallet.getReservedBalance();
        BigDecimal availableBalance = totalBalance.subtract(reservedBalance);

        if (availableBalance.compareTo(payment.getAmount()) < 0) {
            throw new WalletOperationException(
                    String.format("Insufficient available balance. Available: ₹%s, Payment: ₹%s",
                            availableBalance, payment.getAmount())
            );
        }

        // Capture the payment
        BigDecimal paymentAmount = payment.getAmount();
        BigDecimal reservationRemainingBefore = locked.getRemainingAmount();

        // Step 1: Debit wallet total_balance
        lockedWallet.setTotalBalance(totalBalance.subtract(paymentAmount));

        // Step 2: Create DEBIT LedgerEntry
        LedgerEntry ledgerEntry = new LedgerEntry(
                null,  // Will be set after save for payment record
                lockedWallet,
                EntryType.DEBIT,
                paymentAmount,
                "Merchant payment: " + payment.getMerchant().getName()
        );
        ledgerEntry = ledgerEntryRepository.save(ledgerEntry);

        // Step 3: Reduce reservation remaining_amount
        locked.setRemainingAmount(reservationRemainingBefore.subtract(paymentAmount));

        // Step 4: Release entire original remaining_amount from reserved_balance
        lockedWallet.setReservedBalance(
                reservedBalance.subtract(reservationRemainingBefore)
        );

        // Step 5: Set reservation status to CONSUMED
        locked.setStatus(ReservationStatus.CONSUMED);

        // Step 6: Set payment status to CAPTURED
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setLedgerEntry(ledgerEntry);

        // Persist all changes
        walletRepository.save(lockedWallet);
        reservationRepository.save(locked);
        merchantPaymentRepository.save(payment);

        // Send notification
        notificationService.send(
                payment.getUser(),
                NotificationService.TYPE_MERCHANT_PAYMENT_SUCCESS,
                String.format("Payment of ₹%s to %s has been captured successfully.",
                        paymentAmount, payment.getMerchant().getName())
        );
    }

    /**
     * T-077: Handle payment failure.
     *
     * A failed validation or capture must not debit total_balance.
     * Do not consume the reservation.
     * If reservation is still ACTIVE and not expired, preserve it for retry.
     * Record payment as FAILED or REJECTED.
     * Send failure notification.
     * Never leave partially captured state.
     *
     * Call this method in a separate transaction after capture failure.
     *
     * @param payment the payment that failed
     * @param reason the failure reason
     */
    @Transactional
    public void recordPaymentFailure(MerchantPayment payment, String reason) {
        // Reload payment (may be in new transaction)
        MerchantPayment currentPayment = merchantPaymentRepository.findById(payment.getId())
                .orElseThrow(() -> new WalletOperationException("Payment not found"));

        // Set to FAILED (or REJECTED depending on context)
        currentPayment.setStatus(PaymentStatus.FAILED);
        merchantPaymentRepository.save(currentPayment);

        // Send failure notification
        notificationService.send(
                currentPayment.getUser(),
                NotificationService.TYPE_MERCHANT_PAYMENT_FAILED,
                String.format("Payment of ₹%s to %s failed: %s. Reservation preserved for retry.",
                        currentPayment.getAmount(),
                        currentPayment.getMerchant().getName(),
                        reason)
        );
    }

    /**
     * Helper: Convert MerchantPayment entity to response DTO.
     */
    private MerchantPaymentResponse toMerchantPaymentResponse(MerchantPayment payment) {
        Wallet wallet = payment.getWallet();
        BigDecimal totalBalance = wallet.getTotalBalance();
        BigDecimal reservedBalance = wallet.getReservedBalance();
        BigDecimal availableBalance = totalBalance.subtract(reservedBalance);
        if (availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            availableBalance = BigDecimal.ZERO;
        }

        CheckPleaseReservation reservation = payment.getReservation();

        return new MerchantPaymentResponse(
                payment.getId(),
                payment.getReservation().getId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getCreatedAt(),
                totalBalance,
                availableBalance,
                reservedBalance,
                reservation.getRemainingAmount(),
                "Payment " + payment.getStatus().name().toLowerCase()
        );
    }

    /**
     * Helper: Extract authenticated user ID from JWT principal.
     */
    private UUID getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new WalletOperationException("Not authenticated");
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new WalletOperationException("Authenticated user not found"));
        return user.getId();
    }
}

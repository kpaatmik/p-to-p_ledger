package com.paymentledger.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.dto.response.ReservationResponse;
import com.paymentledger.entity.CheckPleaseReservation;
import com.paymentledger.entity.Merchant;
import com.paymentledger.entity.MerchantStatus;
import com.paymentledger.entity.ReservationStatus;
import com.paymentledger.entity.User;
import com.paymentledger.entity.Wallet;
import com.paymentledger.entity.WalletStatus;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.repository.CheckPleaseReservationRepository;
import com.paymentledger.repository.UserRepository;
import com.paymentledger.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

/**
 * Check Please reservation service for T-071 through T-074.
 * Manages reservation creation, validation, listing, and cancellation.
 */
@Service
@RequiredArgsConstructor
public class CheckPleaseService {

    private final CheckPleaseReservationRepository reservationRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final MerchantService merchantService;
    private final NotificationService notificationService;
    private final LedgerService ledgerService;

    @Value("${check.please.reservation.expiry.min-hours:1}")
    private int minExpiryHours;

    @Value("${check.please.reservation.expiry.max-days:30}")
    private int maxExpiryDays;

    /**
     * T-071: Create a new reservation.
     *
     * Validates:
     * - User owns the wallet
     * - Wallet is ACTIVE and not frozen
     * - Merchant exists and is ACTIVE
     * - max_amount is positive
     * - Expiry is in the future
     * - Expiry is at least minExpiryHours, no more than maxExpiryDays
     * - Available balance is sufficient
     *
     * In one atomic transaction:
     * 1. Acquire pessimistic write lock on wallet
     * 2. Recalculate available balance
     * 3. Validate available balance again
     * 4. Increase reserved_balance by max_amount
     * 5. Create reservation with ACTIVE status
     * 6. Send notification
     *
     * @param request request with merchantId, maxAmount, expiresAt
     * @return reservation details with updated wallet balances
     * @throws WalletOperationException on validation failure
     */
    @Transactional
    public ReservationResponse createReservation(
            com.paymentledger.dto.request.ReservationRequest request) {

        UUID userId = getAuthenticatedUserId();
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));

        User user = wallet.getUser();

        // Validate wallet status
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletOperationException("Wallet is not active");
        }

        // Validate max_amount
        if (request.maxAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletOperationException("Max amount must be positive");
        }

        // Validate merchant exists and is ACTIVE
        Merchant merchant = merchantService.getMerchantById(request.merchantId());

        // Validate expiry timing
        Instant now = Instant.now();
        if (!request.expiresAt().isAfter(now)) {
            throw new WalletOperationException("Expiry must be in the future");
        }

        // Validate expiry is within configured bounds
        Instant minExpiry = now.plusSeconds((long) minExpiryHours * 3600);
        Instant maxExpiry = now.plusSeconds((long) maxExpiryDays * 86400);

        if (request.expiresAt().isBefore(minExpiry) || request.expiresAt().isAfter(maxExpiry)) {
            throw new WalletOperationException(
                    String.format("Expiry must be between %d hours and %d days from now",
                            minExpiryHours, maxExpiryDays)
            );
        }

        // Recalculate available balance (wallet is locked)
        BigDecimal totalBalance = wallet.getTotalBalance();
        BigDecimal reservedBalance = wallet.getReservedBalance();
        BigDecimal availableBalance = totalBalance.subtract(reservedBalance);
        if (availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            availableBalance = BigDecimal.ZERO;
        }

        // Validate sufficient available balance
        if (availableBalance.compareTo(request.maxAmount()) < 0) {
            throw new WalletOperationException(
                    String.format("Insufficient available balance. Available: ₹%s, Requested: ₹%s",
                            availableBalance, request.maxAmount())
            );
        }

        // Increase reserved_balance
        wallet.setReservedBalance(reservedBalance.add(request.maxAmount()));
        walletRepository.save(wallet);

        // Create reservation
        CheckPleaseReservation reservation = new CheckPleaseReservation(
                user,
                wallet,
                merchant,
                request.maxAmount(),
                request.expiresAt()
        );
        reservation = reservationRepository.save(reservation);

        // Send notification
        notificationService.send(
                user,
                NotificationService.TYPE_RESERVATION_CREATED,
                String.format("Reservation created for %s with max amount ₹%s",
                        merchant.getName(), request.maxAmount())
        );

        return toReservationResponse(reservation, wallet);
    }

    /**
     * T-072: Validate a reservation before payment capture.
     *
     * Validates:
     * - Reservation exists
     * - Reservation belongs to authenticated user
     * - Reservation status is ACTIVE
     * - Reservation has not expired
     * - Merchant matches the reservation's bound merchant
     * - Payment amount is positive and ≤ remaining_amount
     * - Wallet is ACTIVE and not frozen
     * - Merchant is still ACTIVE
     *
     * @param reservationId reservation ID
     * @param userId authenticated user ID
     * @param merchantId merchant ID for verification
     * @param amount payment amount
     * @throws WalletOperationException on validation failure
     */
    @Transactional(readOnly = true)
    public void validateReservation(UUID reservationId, UUID userId,
                                     UUID merchantId, BigDecimal amount) {

        CheckPleaseReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new WalletOperationException("Reservation not found"));

        // Verify ownership
        if (!reservation.getUser().getId().equals(userId)) {
            throw new WalletOperationException("Reservation does not belong to this user");
        }

        // Verify status is ACTIVE
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new WalletOperationException(
                    String.format("Reservation is not active (status: %s)", reservation.getStatus())
            );
        }

        // Verify not expired
        if (!reservation.getExpiresAt().isAfter(Instant.now())) {
            throw new WalletOperationException("Reservation has expired");
        }

        // Verify merchant matches
        if (!reservation.getMerchant().getId().equals(merchantId)) {
            throw new WalletOperationException(
                    "Merchant does not match reservation's bound merchant"
            );
        }

        // Verify payment amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletOperationException("Payment amount must be positive");
        }

        if (amount.compareTo(reservation.getRemainingAmount()) > 0) {
            throw new WalletOperationException(
                    String.format("Payment amount exceeds remaining reservation amount. " +
                            "Remaining: ₹%s, Payment: ₹%s",
                            reservation.getRemainingAmount(), amount)
            );
        }

        // Verify wallet status
        Wallet wallet = reservation.getWallet();
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletOperationException("Wallet is not active");
        }

        // Verify merchant is still ACTIVE
        if (reservation.getMerchant().getStatus() != MerchantStatus.ACTIVE) {
            throw new WalletOperationException("Merchant is no longer active");
        }
    }

    /**
     * T-073: Get user's reservations (paginated).
     *
     * @param userId authenticated user ID
     * @param status optional status filter
     * @param page page number
     * @param size page size
     * @return paginated list of user's reservations
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getUserReservations(UUID userId,
                                                          ReservationStatus status,
                                                          int page, int size) {
        List<CheckPleaseReservation> reservations;

        if (status != null) {
            reservations = reservationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    userId, status
            );
        } else {
            // Get all, then apply pagination manually (simplified approach)
            org.springframework.data.domain.Page<CheckPleaseReservation> pageResult =
                    reservationRepository.findByUserIdOrderByCreatedAtDesc(
                            userId,
                            org.springframework.data.domain.PageRequest.of(page, size)
                    );
            reservations = pageResult.getContent();
        }

        return reservations.stream()
                .map(r -> toReservationResponse(r, r.getWallet()))
                .toList();
    }

    /**
     * T-073: Get reservation detail by ID.
     *
     * @param reservationId reservation ID
     * @param userId authenticated user ID
     * @return reservation detail
     * @throws WalletOperationException if not found or not owned by user
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservationDetail(UUID reservationId, UUID userId) {
        CheckPleaseReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new WalletOperationException("Reservation not found"));

        // Verify ownership
        if (!reservation.getUser().getId().equals(userId)) {
            throw new WalletOperationException("Reservation does not belong to this user");
        }

        return toReservationResponse(reservation, reservation.getWallet());
    }

    /**
     * T-074: Cancel a reservation.
     *
     * In one transaction:
     * 1. Lock the reservation row
     * 2. Verify ownership
     * 3. If already CANCELLED, return without releasing funds again
     * 4. If ACTIVE, lock the associated wallet
     * 5. Release reservation's remaining_amount from reserved_balance
     * 6. Set reservation status to CANCELLED
     * 7. Send cancellation notification
     *
     * EXPIRED and CONSUMED reservations do not release funds.
     *
     * @param reservationId reservation ID
     * @param userId authenticated user ID
     * @return updated reservation state
     * @throws WalletOperationException on validation failure
     */
    @Transactional
    public ReservationResponse cancelReservation(UUID reservationId, UUID userId) {
        CheckPleaseReservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new WalletOperationException("Reservation not found"));

        // Verify ownership
        if (!reservation.getUser().getId().equals(userId)) {
            throw new WalletOperationException("Reservation does not belong to this user");
        }

        // If already CANCELLED, return current state without releasing funds again
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return toReservationResponse(reservation, reservation.getWallet());
        }

        // Only ACTIVE reservations release funds
        if (reservation.getStatus() == ReservationStatus.ACTIVE) {
            // Lock the wallet and release reserved balance
            Wallet wallet = walletRepository.findByIdWithLock(reservation.getWallet().getId())
                    .orElseThrow(() -> new WalletOperationException("Wallet not found"));

            BigDecimal releaseAmount = reservation.getRemainingAmount();
            wallet.setReservedBalance(wallet.getReservedBalance().subtract(releaseAmount));
            walletRepository.save(wallet);

            reservation.setStatus(ReservationStatus.CANCELLED);
            reservation = reservationRepository.save(reservation);

            // Send notification
            notificationService.send(
                    reservation.getUser(),
                    NotificationService.TYPE_RESERVATION_CANCELLED,
                    String.format("Reservation for %s cancelled. ₹%s released.",
                            reservation.getMerchant().getName(), releaseAmount)
            );

            return toReservationResponse(reservation, wallet);
        }

        // EXPIRED or CONSUMED — no fund release
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation = reservationRepository.save(reservation);

        return toReservationResponse(reservation, reservation.getWallet());
    }

    /**
     * Helper: Mark an ACTIVE reservation as EXPIRED and release reserved balance.
     * Called by the reservation expiry scheduler job.
     *
     * @param reservation the reservation to expire
     */
    @Transactional
    public void expireReservation(CheckPleaseReservation reservation) {
        // Lock the reservation (should already be locked in job context)
        CheckPleaseReservation locked = reservationRepository.findByIdWithLock(reservation.getId())
                .orElseThrow(() -> new WalletOperationException("Reservation not found during expiry"));

        // Lock the wallet
        Wallet wallet = walletRepository.findByIdWithLock(locked.getWallet().getId())
                .orElseThrow(() -> new WalletOperationException("Wallet not found during expiry"));

        // Release reserved balance
        BigDecimal releaseAmount = locked.getRemainingAmount();
        wallet.setReservedBalance(wallet.getReservedBalance().subtract(releaseAmount));
        walletRepository.save(wallet);

        // Mark as EXPIRED
        locked.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(locked);

        // Send notification
        notificationService.send(
                locked.getUser(),
                NotificationService.TYPE_RESERVATION_EXPIRED,
                String.format("Reservation for %s has expired. ₹%s released.",
                        locked.getMerchant().getName(), releaseAmount)
        );
    }

    /**
     * Helper: Convert reservation entity to response DTO.
     */
    private ReservationResponse toReservationResponse(CheckPleaseReservation reservation,
                                                       Wallet wallet) {
        BigDecimal totalBalance = wallet.getTotalBalance();
        BigDecimal reservedBalance = wallet.getReservedBalance();
        BigDecimal availableBalance = totalBalance.subtract(reservedBalance);
        if (availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            availableBalance = BigDecimal.ZERO;
        }

        return new ReservationResponse(
                reservation.getId(),
                reservation.getMerchant().getName(),
                reservation.getMaxAmount(),
                reservation.getRemainingAmount(),
                reservation.getStatus().name(),
                reservation.getExpiresAt(),
                reservation.getCreatedAt(),
                totalBalance,
                availableBalance,
                reservedBalance
        );
    }

    /**
     * T-073: Get user's reservations — controller wrapper that extracts authenticated user.
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getUserReservationsForAuth(ReservationStatus status, int page, int size) {
        UUID userId = getAuthenticatedUserId();
        return getUserReservations(userId, status, page, size);
    }

    /**
     * T-073: Get reservation detail — controller wrapper that extracts authenticated user.
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservationDetailForAuth(UUID reservationId) {
        UUID userId = getAuthenticatedUserId();
        return getReservationDetail(reservationId, userId);
    }

    /**
     * T-074: Cancel reservation — controller wrapper that extracts authenticated user.
     */
    @Transactional
    public ReservationResponse cancelReservationForAuth(UUID reservationId) {
        UUID userId = getAuthenticatedUserId();
        return cancelReservation(reservationId, userId);
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

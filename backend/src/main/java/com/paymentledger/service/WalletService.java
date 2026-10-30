package com.paymentledger.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.dto.response.WalletResponse;
import com.paymentledger.entity.EntryType;
import com.paymentledger.entity.LedgerEntry;
import com.paymentledger.entity.User;
import com.paymentledger.entity.Wallet;
import com.paymentledger.entity.WalletStatus;
import com.paymentledger.exception.InsufficientBalanceException;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.repository.LedgerEntryRepository;
import com.paymentledger.repository.UserRepository;
import com.paymentledger.repository.WalletRepository;

/**
 * Wallet service for Phase 4 operations: load money, get balance,
 * status management, available balance calculation, and atomic debit/credit with pessimistic locking.
 *
 * <p>KEY INVARIANT: available_balance = total_balance - reserved_balance
 * This is NEVER stored in the database — always computed in the service layer.
 */
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final BigDecimal maxLoadAmount;

    public WalletService(WalletRepository walletRepository,
                         LedgerEntryRepository ledgerEntryRepository,
                         AuditLogService auditLogService,
                         UserRepository userRepository,
                         @Value("${wallet.load.max-amount}") String maxLoadAmount) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.maxLoadAmount = new BigDecimal(maxLoadAmount);
    }

    // ── T-040: Load Money ──────────────────────────────────────────────────

    /**
     * T-040: Loads money into the authenticated user's wallet.
     * Increases total_balance by the amount.
     * Creates a CREDIT ledger entry.
     *
     * @param amount the amount to load (must be positive and within wallet.load.max-amount)
     * @return updated wallet response
     * @throws WalletOperationException if wallet not found, amount invalid, or wallet not ACTIVE
     */
    @Transactional
    public WalletResponse loadMoney(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletOperationException("Load amount must be positive");
        }
        if (amount.compareTo(maxLoadAmount) > 0) {
            throw new WalletOperationException(
                    "Load amount exceeds maximum of ₹" + maxLoadAmount);
        }

        UUID userId = getAuthenticatedUserId();
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletOperationException("Wallet is not active and cannot load money");
        }

        // Increase total balance
        wallet.setTotalBalance(wallet.getTotalBalance().add(amount));
        walletRepository.save(wallet);

        // Create CREDIT ledger entry (no transaction yet, so null is allowed)
        ledgerEntryRepository.save(new LedgerEntry(
                null,  // no transaction for load operations
                wallet,
                EntryType.CREDIT,
                amount,
                "Load money"
        ));

        return getWalletResponse(wallet);
    }

    // ── T-041: Get Wallet ──────────────────────────────────────────────────

    /**
     * T-041: Retrieves the authenticated user's wallet with balance breakdown.
     *
     * @return wallet response including available balance (computed) and today's spending
     * @throws WalletOperationException if wallet not found
     */
    @Transactional(readOnly = true)
    public WalletResponse getWallet() {
        UUID userId = getAuthenticatedUserId();
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));
        return getWalletResponse(wallet);
    }

    // ── T-042: Wallet Status Management ────────────────────────────────────

    /**
     * Admin action: freeze a wallet.
     * Requires @PreAuthorize("hasRole('ADMIN')" at the controller level.
     * Creates an audit log entry.
     *
     * @param walletId the wallet to freeze
     * @throws WalletOperationException if wallet not found
     */
    @Transactional
    public void freezeWallet(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));

        wallet.setStatus(WalletStatus.FROZEN);
        walletRepository.save(wallet);

        // Log the admin action
        UUID adminId = getAuthenticatedUserId();
        auditLogService.recordAdminAction(
        adminId,
        "WALLET_FROZEN",
        "Wallet",
        wallet.getId(),
        "Wallet frozen by administrator"
);
    }

    /**
     * Admin action: unfreeze a wallet.
     * Requires @PreAuthorize("hasRole('ADMIN')" at the controller level.
     * Creates an audit log entry.
     *
     * @param walletId the wallet to unfreeze
     * @throws WalletOperationException if wallet not found or not FROZEN
     */
    @Transactional
    public void unfreezeWallet(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));

        if (wallet.getStatus() != WalletStatus.FROZEN) {
            throw new WalletOperationException("Wallet is not frozen");
        }

        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);

        // Log the admin action
        UUID adminId = getAuthenticatedUserId();
     auditLogService.recordAdminAction(
        adminId,
        "WALLET_UNFROZEN",
        "Wallet",
        wallet.getId(),
        "Wallet unfrozen by administrator"
);
    }

    /**
     * Owner action: reactivate their own INACTIVE wallet to ACTIVE.
     * Non-admin users can only reactivate their own wallets.
     *
     * @throws WalletOperationException if wallet not found, not owned by caller, or not INACTIVE
     */
    @Transactional
    public void reactivateOwnWallet() {
        UUID userId = getAuthenticatedUserId();
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));

        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new WalletOperationException("Frozen wallet cannot be reactivated by owner");
        }
        if (wallet.getStatus() != WalletStatus.INACTIVE) {
            throw new WalletOperationException("Wallet is already active");
        }

        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);
    }

    // ── T-043: Available Balance Calculation ──────────────────────────────

    /**
     * T-043: Computes available balance as total_balance - reserved_balance.
     * Single source of truth for available balance.
     * Reads fresh values from the database.
     *
     * @param walletId the wallet ID
     * @return available balance (total - reserved); zero if negative (defensive)
     */
    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));

        BigDecimal available = wallet.getTotalBalance().subtract(wallet.getReservedBalance());
        // Defensive: if reserved_balance > total_balance (should never happen due to CHECK constraint),
        // return zero to prevent negative balance
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return available;
    }

    // ── T-044: Atomic Debit/Credit with Pessimistic Locking ──────────────

    /**
     * T-044: Atomically debits a wallet with pessimistic row locking.
     * Acquires SELECT FOR UPDATE lock, checks sufficient available balance, then debits.
     * Throws InsufficientBalanceException if balance is insufficient.
     * Does NOT create ledger entries — caller is responsible.
     *
     * @param walletId the wallet ID
     * @param amount the amount to debit
     * @throws InsufficientBalanceException if available balance < amount
     * @throws WalletOperationException if wallet not found
     */
    @Transactional
    public void debitWallet(UUID walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));

        BigDecimal available = wallet.getTotalBalance().subtract(wallet.getReservedBalance());
        if (available.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(available, amount);
        }

        wallet.setTotalBalance(wallet.getTotalBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    /**
     * T-044: Atomically credits a wallet with pessimistic row locking.
     * Acquires SELECT FOR UPDATE lock, then credits.
     * Does NOT create ledger entries — caller is responsible.
     *
     * @param walletId the wallet ID
     * @param amount the amount to credit
     * @throws WalletOperationException if wallet not found
     */
    @Transactional
    public void creditWallet(UUID walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));

        wallet.setTotalBalance(wallet.getTotalBalance().add(amount));
        walletRepository.save(wallet);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Gets a wallet with pessimistic lock by wallet ID.
     * Used for operations that need to acquire the lock before reading the wallet.
     *
     * @param walletId the wallet ID
     * @return the wallet with lock acquired
     * @throws WalletOperationException if wallet not found
     */
    @Transactional
    public Wallet getWalletWithLock(UUID walletId) {
        return walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));
    }

    /**
     * Gets a wallet with pessimistic lock by user ID.
     * Used in transfer flows where we have the user ID (not the wallet ID).
     *
     * @param userId the user's ID
     * @return the wallet with lock acquired
     * @throws WalletOperationException if wallet not found
     */
    @Transactional
    public Wallet getWalletWithLockByUserId(UUID userId) {
        return walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));
    }

    /**
     * Gets a wallet by ID without lock (read-only use).
     * Used for non-modifying operations.
     *
     * @param walletId the wallet ID
     * @return the wallet
     * @throws WalletOperationException if wallet not found
     */
    @Transactional(readOnly = true)
    public Wallet getWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));
    }

    /**
     * Gets a wallet by user ID without lock (read-only use).
     * Used in transfer flows where we have the user ID (not the wallet ID).
     *
     * @param userId the user's ID
     * @return the wallet
     * @throws WalletOperationException if wallet not found
     */
    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletOperationException("Wallet not found"));
    }

    /**
     * Extracts the authenticated user's ID from the JWT principal in SecurityContextHolder.
     * Never trust client-supplied user IDs.
     * The principal is the email (set by JwtAuthFilter), so we resolve to User to get the ID.
     *
     * @return the authenticated user's ID
     * @throws WalletOperationException if not authenticated or user not found
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

    /**
     * Constructs a WalletResponse from a wallet entity.
     * Computes available balance and today's spending.
     *
     * @param wallet the wallet entity
     * @return wallet response with all fields populated
     */
    private WalletResponse getWalletResponse(Wallet wallet) {
        BigDecimal totalBalance = wallet.getTotalBalance();
        BigDecimal reservedBalance = wallet.getReservedBalance();
        BigDecimal availableBalance = totalBalance.subtract(reservedBalance);
        if (availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            availableBalance = BigDecimal.ZERO;
        }

        // Today's spending: sum of DEBIT entries for this wallet created today (UTC)
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfToday = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal todaySpending = ledgerEntryRepository.sumByWalletAndTypeSince(
                wallet.getId(),
                EntryType.DEBIT,
                startOfToday
        );

        return new WalletResponse(
                totalBalance,
                availableBalance,
                reservedBalance,
                wallet.getStatus().name(),
                todaySpending
        );
    }
}

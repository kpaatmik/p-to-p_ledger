package com.paymentledger.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.entity.LoginAttempt;
import com.paymentledger.entity.User;
import com.paymentledger.entity.UserRole;
import com.paymentledger.entity.Wallet;
import com.paymentledger.entity.WalletStatus;
import com.paymentledger.exception.AccountLockedException;
import com.paymentledger.exception.DuplicateEmailException;
import com.paymentledger.exception.DuplicatePhoneException;
import com.paymentledger.repository.LoginAttemptRepository;
import com.paymentledger.repository.UserRepository;
import com.paymentledger.repository.WalletRepository;
import com.paymentledger.security.JwtUtil;

/**
 * Handles user registration, login, failed-login tracking, and account locking.
 *
 * <p>Security invariants:
 * <ul>
 *   <li>Passwords and PINs are BCrypt-hashed before storage; plaintext is never persisted.</li>
 *   <li>Secrets (passwords, PINs, JWTs) are never logged.</li>
 *   <li>Login errors use a generic message to prevent email-enumeration attacks.</li>
 *   <li>PIN verification failures and login failures are tracked separately.
 *       Only login failures trigger account locking (per steering rules).</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final int  MAX_FAILED_ATTEMPTS  = 5;
    private static final long LOCK_DURATION_SECONDS = 30 * 60L; // 30 minutes

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;

    public AuthService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       LoginAttemptRepository loginAttemptRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.notificationService = notificationService;
    }

    // ── Result record ──────────────────────────────────────────────────────

    /**
     * Carries the JWT and safe user fields back to the controller so no
     * second DB fetch is needed after login.
     */
    public record LoginResult(String token, UUID userId, String email, String role) {}

    // ── Registration (T-032) ───────────────────────────────────────────────

    /**
     * Creates a new user and their single ACTIVE wallet atomically.
     *
     * @throws DuplicateEmailException if the email is already registered
     * @throws DuplicatePhoneException if the phone is already registered
     */
    @Transactional
    public User register(String name, String email, String phone,
                         String plainPassword, String plainPin) {

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        if (userRepository.existsByPhone(phone)) {
            throw new DuplicatePhoneException(phone);
        }

        User user = new User(
                name, email, phone,
                passwordEncoder.encode(plainPassword),
                passwordEncoder.encode(plainPin),
                UserRole.USER);
        userRepository.save(user);

        Wallet wallet = new Wallet(user);
        wallet.setTotalBalance(BigDecimal.ZERO);
        wallet.setReservedBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);

        return user;
    }

    // ── Login (T-033 + T-034) ──────────────────────────────────────────────

    /**
     * Authenticates user, enforces account locking, and returns a JWT on success.
     *
     * @param email       the user's email address
     * @param plainPassword the plain-text password submitted by the user
     * @param ipAddress   client IP for LoginAttempt recording
     * @return {@link LoginResult} containing the JWT and safe user details
     * @throws AccountLockedException  if the account is currently locked
     * @throws BadCredentialsException for any authentication failure (generic message)
     */
    @Transactional
    public LoginResult login(String email, String plainPassword, String ipAddress) {

        // Generic error to prevent email enumeration
        final String GENERIC_ERR = "Invalid email or password";

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            throw new BadCredentialsException(GENERIC_ERR);
        }

        // Reject if locked and lock has not yet expired
        if (user.isAccountLocked()) {
            Instant expiry = user.getLockExpiresAt();
            if (expiry != null && Instant.now().isBefore(expiry)) {
                throw new AccountLockedException(expiry);
            }
            // Lock window passed — auto-clear without waiting for the unlock job
            user.setAccountLocked(false);
            user.setLockExpiresAt(null);
            user.setFailedLoginCount(0);
            userRepository.save(user);
        }

        if (!passwordEncoder.matches(plainPassword, user.getPasswordHash())) {
            recordFailedLogin(user, ipAddress);
            throw new BadCredentialsException(GENERIC_ERR);
        }

        // ── Successful login ──────────────────────────────────────────────
        user.setFailedLoginCount(0);
        userRepository.save(user);
        loginAttemptRepository.save(new LoginAttempt(user, true, ipAddress));

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new LoginResult(token, user.getId(), user.getEmail(), user.getRole().name());
    }

    // ── Failed-login tracking (T-034) ──────────────────────────────────────

    private void recordFailedLogin(User user, String ipAddress) {
        loginAttemptRepository.save(new LoginAttempt(user, false, ipAddress));

        int newCount = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(newCount);

        if (newCount >= MAX_FAILED_ATTEMPTS) {
            Instant lockUntil = Instant.now().plusSeconds(LOCK_DURATION_SECONDS);
            user.setAccountLocked(true);
            user.setLockExpiresAt(lockUntil);
            userRepository.save(user);

            notificationService.send(
                    user,
                    NotificationService.TYPE_ACCOUNT_LOCKED,
                    "Your account has been temporarily locked for 30 minutes after "
                    + MAX_FAILED_ATTEMPTS + " consecutive failed login attempts.");
        } else {
            userRepository.save(user);
        }
    }
}

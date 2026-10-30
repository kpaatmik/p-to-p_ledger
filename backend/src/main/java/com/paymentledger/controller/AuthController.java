package com.paymentledger.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentledger.dto.request.LoginRequest;
import com.paymentledger.dto.request.RegisterRequest;
import com.paymentledger.dto.response.AuthResponse;
import com.paymentledger.dto.response.RegisterResponse;
import com.paymentledger.entity.User;
import com.paymentledger.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Authentication endpoints — publicly accessible (no JWT required).
 *
 * <pre>
 *   POST /auth/register  → create account + ACTIVE wallet
 *   POST /auth/login     → authenticate, receive JWT
 * </pre>
 *
 * Error responses are handled by {@code GlobalExceptionHandler}:
 * HTTP 400 for validation failures, HTTP 409 for duplicate accounts,
 * HTTP 423 for locked accounts, HTTP 401 for bad credentials.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     * Creates the user and their ACTIVE wallet atomically.
     * Returns HTTP 201 on success.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.register(
                request.name(),
                request.email(),
                request.phone(),
                request.password(),
                request.pin());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new RegisterResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getName(),
                        user.getRole().name(),
                        "Registration successful. You may now sign in."));
    }

    /**
     * Authenticate with email + password.
     * Returns HTTP 200 with JWT on success.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthService.LoginResult result =
                authService.login(request.email(), request.password(),
                                  extractClientIp(httpRequest));

        return ResponseEntity.ok(
                new AuthResponse(result.token(), result.userId(),
                                 result.email(), result.role()));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Resolves the real client IP, respecting X-Forwarded-For when present. */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

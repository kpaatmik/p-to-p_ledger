package com.paymentledger.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.paymentledger.dto.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Centralised exception handler. Translates exceptions to consistent
 * ErrorResponse JSON for all controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Jakarta Bean Validation failures on @Valid request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "One or more fields failed validation",
                req.getRequestURI());
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ErrorResponse.FieldError(
                        fe.getField(),
                        fe.getRejectedValue() == null ? null : fe.getRejectedValue().toString(),
                        fe.getDefaultMessage()))
                .toList();
        body.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /** Constraint violations on @Validated method parameters. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Constraint Violation",
                ex.getMessage(),
                req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    /** Spring Security — unauthenticated access. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ErrorResponse(401, "Unauthorized", ex.getMessage(), req.getRequestURI()));
    }

    /** Spring Security — authorised user lacking required role. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponse(403, "Forbidden", ex.getMessage(), req.getRequestURI()));
    }

    /** Catch-all for unhandled runtime exceptions. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse(500, "Internal Server Error",
                        "An unexpected error occurred", req.getRequestURI()));
    }

    // ── Phase 3 auth exceptions ────────────────────────────────────────────

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(409, "Conflict", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(DuplicatePhoneException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePhone(
            DuplicatePhoneException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(409, "Conflict", ex.getMessage(), req.getRequestURI()));
    }

    /** HTTP 423 Locked — account is temporarily locked after failed login attempts. */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(
            AccountLockedException ex, HttpServletRequest req) {
        ErrorResponse body = new ErrorResponse(
                423, "Account Locked", ex.getMessage(), req.getRequestURI());
        // Include lock expiry as extra data via a small wrapper
        body.setFieldErrors(List.of(
                new ErrorResponse.FieldError("lockedUntil", ex.getLockedUntil().toString(),
                        "Account is temporarily locked")));
        return ResponseEntity.status(423).body(body);
    }

    // ── Phase 7 Check Please exceptions ────────────────────────────────────

    /** HTTP 401 Unauthorized — incorrect transaction PIN for merchant payments. */
    @ExceptionHandler(InvalidPinException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPin(
            InvalidPinException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ErrorResponse(401, "Unauthorized", ex.getMessage(), req.getRequestURI()));
    }

    // ── Phase 4 wallet exceptions ──────────────────────────────────────────

    /** HTTP 422 Unprocessable Entity — wallet operation error (not found, invalid status). */
    @ExceptionHandler(WalletOperationException.class)
    public ResponseEntity<ErrorResponse> handleWalletOperation(
            WalletOperationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                new ErrorResponse(422, "Wallet Operation Failed", ex.getMessage(), req.getRequestURI()));
    }

    /** HTTP 402 Payment Required — insufficient wallet balance for debit. */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException ex, HttpServletRequest req) {
        ErrorResponse body = new ErrorResponse(
                402, "Insufficient Balance", ex.getMessage(), req.getRequestURI());
        body.setFieldErrors(List.of(
                new ErrorResponse.FieldError("available", ex.getAvailableBalance().toString(), "Available balance"),
                new ErrorResponse.FieldError("requested", ex.getRequestedAmount().toString(), "Requested amount")));
        return ResponseEntity.status(402).body(body);
    }
}

package com.paymentledger.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT utility: generate, validate and parse HS256 tokens.
 *
 * <p>Secret is read from {@code jwt.secret} in application.properties,
 * which is sourced from the {@code JWT_SECRET} environment variable.
 * The secret is NEVER logged.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration.ms:86400000}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // ── Token generation ───────────────────────────────────────────────────

    /**
     * Generates a signed HS256 JWT containing userId, email and role claims.
     * The token is valid for {@code jwt.expiration.ms} milliseconds.
     */
    public String generateToken(UUID userId, String email, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("role",   role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // ── Token validation ───────────────────────────────────────────────────

    /**
     * Returns {@code true} if the token has a valid signature and has not expired.
     * Any parse or signature error returns {@code false}; nothing is logged.
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ── Claim extraction ───────────────────────────────────────────────────

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        String raw = parseClaims(token).get("userId", String.class);
        return UUID.fromString(raw);
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

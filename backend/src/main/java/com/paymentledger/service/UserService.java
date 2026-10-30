package com.paymentledger.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.dto.response.UserSearchResponse;
import com.paymentledger.entity.User;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.repository.UserRepository;

/**
 * User service for T-050 (recipient search) and helper methods.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * T-050: Searches for a user by exact email or phone match.
     * Excludes the authenticated requester from results.
     * Returns masked email and phone for privacy.
     *
     * @param query email or phone number to search for
     * @return UserSearchResponse with masked contact info, or empty Optional
     * @throws WalletOperationException if query is empty or too short
     */
    @Transactional(readOnly = true)
    public Optional<UserSearchResponse> searchUserByEmailOrPhone(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new WalletOperationException("Search query cannot be empty");
        }

        String trimmed = query.trim();
        if (trimmed.length() < 3) {
            throw new WalletOperationException("Search query must be at least 3 characters");
        }

        UUID requesterId = getAuthenticatedUserId();

        // Try as email first (contains @)
        if (trimmed.contains("@")) {
            Optional<User> user = userRepository.findByEmail(trimmed);
            if (user.isPresent() && !user.get().getId().equals(requesterId)) {
                return Optional.of(toSearchResponse(user.get()));
            }
            return Optional.empty();
        }

        // Try as phone
        Optional<User> user = userRepository.findByPhone(trimmed);
        if (user.isPresent() && !user.get().getId().equals(requesterId)) {
            return Optional.of(toSearchResponse(user.get()));
        }

        return Optional.empty();
    }

    /**
     * Helper: Gets the authenticated user ID from JWT principal.
     */
    protected UUID getAuthenticatedUserId() {
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
     * Helper: Gets the authenticated user by ID.
     */
    public User getAuthenticatedUser() {
        UUID userId = getAuthenticatedUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new WalletOperationException("Authenticated user not found"));
    }

    /**
     * Converts User entity to UserSearchResponse with masked contact info.
     */
    private UserSearchResponse toSearchResponse(User user) {
        return new UserSearchResponse(
                user.getId(),
                user.getName(),
                UserSearchResponse.maskEmail(user.getEmail()),
                UserSearchResponse.maskPhone(user.getPhone())
        );
    }
}

package com.paymentledger.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentledger.dto.response.NotificationResponse;
import com.paymentledger.service.NotificationService;
import com.paymentledger.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * User notification endpoints (Phase 9, T-101 and T-102).
 * 
 * Endpoints:
 * - GET /notifications — retrieve paginated notifications for authenticated user
 * - PATCH /notifications/{id}/read — mark notification as read
 * - GET /notifications/unread-count — count unread notifications
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * T-101: Get paginated notifications for the authenticated user.
     * 
     * Query parameters:
     * - page: page number (0-indexed, default 0)
     * - size: page size (default 20)
     * - sort: sort criteria (default: createdAt,desc)
     * 
     * @param pageable pagination parameters
     * @return paginated list of user's notifications
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(Pageable pageable) {
        UUID userId = userService.getAuthenticatedUser().getId();
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * T-101: Mark a notification as read.
     * 
     * Ownership is verified: only the notification's owner can mark it as read.
     * Repeated requests are idempotent: marking an already-read notification succeeds silently.
     * 
     * @param notificationId the notification ID
     * @return 204 No Content on success
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId) {
        UUID userId = userService.getAuthenticatedUser().getId();
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * T-102: Get unread notification count for the authenticated user.
     * 
     * @return JSON response: { "count": N }
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        UUID userId = userService.getAuthenticatedUser().getId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    /**
     * Response DTO for unread count endpoint.
     */
    public record UnreadCountResponse(long count) {}
}

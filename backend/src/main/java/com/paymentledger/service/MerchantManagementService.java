package com.paymentledger.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymentledger.dto.request.MerchantRegistrationRequest;
import com.paymentledger.dto.request.MerchantStatusUpdateRequest;
import com.paymentledger.dto.response.MerchantResponse;
import com.paymentledger.entity.Merchant;
import com.paymentledger.entity.MerchantStatus;
import com.paymentledger.entity.User;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.repository.MerchantRepository;
import com.paymentledger.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Merchant management service for admin operations (Phase 9, T-110).
 */
@Service
@RequiredArgsConstructor
public class MerchantManagementService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /**
     * T-110: Register a new merchant.
     * 
     * - Validates name and email.
     * - Enforces email uniqueness.
     * - Sets initial status to ACTIVE.
     * - Records the admin ID who registered the merchant.
     * - Uses the authenticated admin principal, never client-supplied admin ID.
     * 
     * @param request merchant registration request
     * @param adminId the authenticated admin ID
     * @return merchant response
     * @throws WalletOperationException if email already exists
     */
    @Transactional
    public MerchantResponse registerMerchant(MerchantRegistrationRequest request, UUID adminId) {
        // Check email uniqueness
        if (merchantRepository.existsByEmail(request.email())) {
            throw new WalletOperationException("Merchant email already registered: " + request.email());
        }

        // Get admin user
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found: " + adminId));

        // Create merchant using existing constructor
        Merchant merchant = new Merchant(request.name(), request.email(), admin);
        merchant.setStatus(MerchantStatus.ACTIVE);

        Merchant saved = merchantRepository.save(merchant);

        // Audit log
        auditLogService.recordAdminAction(
                adminId,
                "REGISTER_MERCHANT",
                "MERCHANT",
                saved.getId(),
                "Registered merchant: " + saved.getEmail()
        );

        return new MerchantResponse(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getStatus().toString(),
                admin.getEmail(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    /**
     * T-110: Update merchant status.
     * 
     * - Validates status transition.
     * - Records audit log for status change.
     * - Does not allow direct suspension-to-active transitions without explicit approval.
     * 
     * @param merchantId the merchant ID
     * @param request status update request
     * @param adminId the authenticated admin ID
     * @return updated merchant response
     * @throws EntityNotFoundException if merchant not found
     * @throws WalletOperationException if invalid status transition
     */
    @Transactional
    public MerchantResponse updateMerchantStatus(UUID merchantId, MerchantStatusUpdateRequest request, UUID adminId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found: " + adminId));

        // Validate new status
        MerchantStatus newStatus;
        try {
            newStatus = MerchantStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WalletOperationException("Invalid merchant status: " + request.status());
        }

        // Check for invalid status transitions
        MerchantStatus currentStatus = merchant.getStatus();
        
        // Suspended merchants cannot be directly activated without explicit approval
        if (currentStatus == MerchantStatus.SUSPENDED && newStatus == MerchantStatus.ACTIVE) {
            throw new WalletOperationException(
                    "Cannot directly activate a suspended merchant. Requires explicit review and approval."
            );
        }

        // Update status
        merchant.setStatus(newStatus);
        Merchant updated = merchantRepository.save(merchant);

        // Audit log
        auditLogService.recordAdminAction(
                adminId,
                "UPDATE_MERCHANT_STATUS",
                "MERCHANT",
                merchantId,
                "Changed merchant status from " + currentStatus + " to " + newStatus + 
                " for merchant " + merchant.getEmail()
        );

        return new MerchantResponse(
                updated.getId(),
                updated.getName(),
                updated.getEmail(),
                updated.getStatus().toString(),
                admin.getEmail(),
                updated.getCreatedAt(),
                updated.getUpdatedAt()
        );
    }
}

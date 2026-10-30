package com.paymentledger.service;

import com.paymentledger.dto.response.MerchantResponse;
import com.paymentledger.entity.Merchant;
import com.paymentledger.entity.MerchantStatus;
import com.paymentledger.exception.WalletOperationException;
import com.paymentledger.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Merchant service for Check Please operations.
 * Handles merchant search, validation, and retrieval.
 */
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    /**
     * T-070: Search for ACTIVE merchants by optional case-insensitive name.
     *
     * @param searchTerm optional partial name match (case-insensitive)
     * @return list of ACTIVE merchants matching the search criteria
     */
    @Transactional(readOnly = true)
    public List<MerchantResponse> searchMerchants(String searchTerm) {
        List<Merchant> merchants;

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // No search term — return all ACTIVE merchants
            merchants = merchantRepository.findByStatusOrderByNameAsc(MerchantStatus.ACTIVE);
        } else {
            // Case-insensitive partial name search — ACTIVE only
            merchants = merchantRepository.findByStatusAndNameContainingIgnoreCaseOrderByNameAsc(
                    MerchantStatus.ACTIVE,
                    searchTerm.trim()
            );
        }

        return merchants.stream()
                .map(m -> new MerchantResponse(m.getId(), m.getName(), m.getStatus().name()))
                .toList();
    }

    /**
     * Helper: Get merchant by ID and verify ACTIVE status.
     * Used during reservation validation and payment processing.
     *
     * @param merchantId the merchant ID
     * @return the merchant entity
     * @throws WalletOperationException if merchant not found or not ACTIVE
     */
    @Transactional(readOnly = true)
    public Merchant getMerchantById(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new WalletOperationException("Merchant not found"));

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new WalletOperationException("Merchant is not active");
        }

        return merchant;
    }

    /**
     * Helper: Get merchant by ID without status check.
     * Used for validation that merchant exists (status checked separately).
     *
     * @param merchantId the merchant ID
     * @return the merchant entity
     * @throws WalletOperationException if merchant not found
     */
    @Transactional(readOnly = true)
    public Merchant getMerchantByIdUnchecked(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new WalletOperationException("Merchant not found"));
    }
}

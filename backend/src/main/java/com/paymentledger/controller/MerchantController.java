package com.paymentledger.controller;

import com.paymentledger.dto.response.MerchantResponse;
import com.paymentledger.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Merchant endpoints for Check Please (Phase 7).
 *
 * T-070: Merchant search/list endpoint.
 * - Returns only ACTIVE merchants
 * - Supports optional case-insensitive name search
 * - Returns merchant ID, name, status
 */
@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * T-070: Get list of ACTIVE merchants.
     *
     * Query parameters:
     * - search: optional case-insensitive partial name match
     *
     * @param search optional search term for merchant name
     * @return list of ACTIVE merchants
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<MerchantResponse>> getMerchants(
            @RequestParam(required = false) String search) {
        List<MerchantResponse> merchants = merchantService.searchMerchants(search);
        return ResponseEntity.ok(merchants);
    }
}

package com.paymentledger.service;

import com.paymentledger.entity.EntryType;
import com.paymentledger.entity.LedgerEntry;
import com.paymentledger.entity.Transaction;
import com.paymentledger.entity.Wallet;
import com.paymentledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Ledger entry service for transaction accounting.
 * Creates immutable ledger records for all wallet debits and credits.
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    /**
     * Create a debit ledger entry (outgoing funds).
     *
     * @param transaction the transaction (may be null for non-transaction debits like loads)
     * @param wallet the wallet being debited
     * @param amount the debit amount
     * @param description the entry description
     * @return the created ledger entry
     */
    @Transactional
    public LedgerEntry createDebit(Transaction transaction, Wallet wallet,
                                    BigDecimal amount, String description) {
        LedgerEntry entry = new LedgerEntry(transaction, wallet, EntryType.DEBIT, amount, description);
        return ledgerEntryRepository.save(entry);
    }

    /**
     * Create a credit ledger entry (incoming funds).
     *
     * @param transaction the transaction (may be null for non-transaction credits like loads)
     * @param wallet the wallet being credited
     * @param amount the credit amount
     * @param description the entry description
     * @return the created ledger entry
     */
    @Transactional
    public LedgerEntry createCredit(Transaction transaction, Wallet wallet,
                                     BigDecimal amount, String description) {
        LedgerEntry entry = new LedgerEntry(transaction, wallet, EntryType.CREDIT, amount, description);
        return ledgerEntryRepository.save(entry);
    }
}

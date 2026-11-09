package com.paymesh.transaction_service.service;

import com.paymesh.transaction_service.entity.Wallet;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletService {

    /** Idempotently ensures a zero-balance wallet exists for the given user. */
    Wallet provisionWallet(Long userId);

    /** Adds funds to the authenticated user's wallet, recording a CREDIT ledger entry. */
    Wallet topUp(Long userId, BigDecimal amount);

    Optional<Wallet> findWallet(Long userId);
}
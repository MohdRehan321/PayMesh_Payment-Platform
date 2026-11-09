package com.paymesh.transaction_service.service;

import com.paymesh.transaction_service.entity.LedgerEntry;
import com.paymesh.transaction_service.entity.Wallet;
import com.paymesh.transaction_service.repository.LedgerEntryRepository;
import com.paymesh.transaction_service.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public WalletServiceImpl(WalletRepository walletRepository,
                             LedgerEntryRepository ledgerEntryRepository) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    @Transactional
    public Wallet provisionWallet(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Provisioning new zero-balance wallet for userId={}", userId);
                    return walletRepository.save(new Wallet(userId, BigDecimal.ZERO));
                });
    }

    @Override
    @Transactional
    public Wallet topUp(Long userId, BigDecimal amount) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(userId, BigDecimal.ZERO)));

        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet saved = walletRepository.save(wallet);

        ledgerEntryRepository.save(new LedgerEntry(null, userId, amount, "CREDIT"));

        log.info("Wallet topped up: userId={}, newBalance={}", userId, saved.getBalance());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Wallet> findWallet(Long userId) {
        return walletRepository.findByUserId(userId);
    }
}
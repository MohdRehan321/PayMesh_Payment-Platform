package com.paymesh.transaction_service.service;

import com.paymesh.transaction_service.entity.LedgerEntry;
import com.paymesh.transaction_service.entity.Wallet;
import com.paymesh.transaction_service.repository.LedgerEntryRepository;
import com.paymesh.transaction_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private WalletServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WalletServiceImpl(walletRepository, ledgerEntryRepository);
    }

    @Test
    void provisionWallet_createsZeroBalanceWalletWhenAbsent() {
        given(walletRepository.findByUserId(7L)).willReturn(Optional.empty());
        given(walletRepository.save(any(Wallet.class))).willAnswer(inv -> inv.getArgument(0));

        Wallet wallet = service.provisionWallet(7L);

        assertEquals(7L, wallet.getUserId());
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
    }

    @Test
    void provisionWallet_returnsExistingWalletWithoutSaving() {
        Wallet existing = new Wallet(7L, new BigDecimal("42"));
        given(walletRepository.findByUserId(7L)).willReturn(Optional.of(existing));

        Wallet wallet = service.provisionWallet(7L);

        assertEquals(existing, wallet);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void topUp_incrementsBalanceAndWritesCreditLedgerEntry() {
        Wallet wallet = new Wallet(7L, new BigDecimal("100"));
        given(walletRepository.findByUserIdForUpdate(7L)).willReturn(Optional.of(wallet));
        given(walletRepository.save(any(Wallet.class))).willAnswer(inv -> inv.getArgument(0));

        Wallet updated = service.topUp(7L, new BigDecimal("900"));

        assertEquals(0, new BigDecimal("1000").compareTo(updated.getBalance()));

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());
        LedgerEntry entry = captor.getValue();
        assertEquals("CREDIT", entry.getEntryType());
        assertEquals(0, new BigDecimal("900").compareTo(entry.getAmount()));
        assertEquals(7L, entry.getUserId());
        assertNull(entry.getTransactionId());
    }

    @Test
    void topUp_createsWalletWhenNoneExists() {
        given(walletRepository.findByUserIdForUpdate(7L)).willReturn(Optional.empty());
        given(walletRepository.save(any(Wallet.class))).willAnswer(inv -> inv.getArgument(0));

        Wallet updated = service.topUp(7L, new BigDecimal("250"));

        assertEquals(0, new BigDecimal("250").compareTo(updated.getBalance()));
    }

    @Test
    void topUp_rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> service.topUp(7L, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> service.topUp(7L, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> service.topUp(7L, null));
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void provisionWallet_rejectsNullUserId() {
        assertThrows(IllegalArgumentException.class, () -> service.provisionWallet(null));
    }

    @Test
    void findWallet_delegatesToRepository() {
        Wallet wallet = new Wallet(7L, BigDecimal.TEN);
        given(walletRepository.findByUserId(7L)).willReturn(Optional.of(wallet));

        Optional<Wallet> result = service.findWallet(7L);

        assertTrue(result.isPresent());
        assertEquals(wallet, result.get());
    }
}
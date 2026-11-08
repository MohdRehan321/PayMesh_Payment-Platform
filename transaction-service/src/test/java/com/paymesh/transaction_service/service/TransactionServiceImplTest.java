package com.paymesh.transaction_service.service;

import com.paymesh.transaction_service.dto.TransferRequest;
import com.paymesh.transaction_service.entity.LedgerEntry;
import com.paymesh.transaction_service.entity.Transaction;
import com.paymesh.transaction_service.entity.Wallet;
import com.paymesh.transaction_service.exception.AccountNotFoundException;
import com.paymesh.transaction_service.exception.DuplicateTransactionException;
import com.paymesh.transaction_service.exception.InsufficientFundsException;
import com.paymesh.transaction_service.kafka.KafkaEventProducer;
import com.paymesh.transaction_service.repository.LedgerEntryRepository;
import com.paymesh.transaction_service.repository.TransactionRepository;
import com.paymesh.transaction_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository repository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private KafkaEventProducer kafkaEventProducer;

    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(repository, walletRepository, ledgerEntryRepository, kafkaEventProducer);
    }

    private TransferRequest request(Long senderId, Long receiverId, String amount, String key) {
        TransferRequest r = new TransferRequest();
        r.setSenderId(senderId);
        r.setReceiverId(receiverId);
        r.setAmount(amount == null ? null : new BigDecimal(amount));
        r.setIdempotencyKey(key);
        return r;
    }

    private void persistReturnsEntity() {
        given(repository.save(any(Transaction.class))).willAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(99L);
            }
            return t;
        });
    }

    @Test
    void validTransfer_debitsSender_creditsReceiver_andPersistsLedger() {
        Wallet sender = new Wallet(12L, new BigDecimal("1000"));
        Wallet receiver = new Wallet(11L, new BigDecimal("0"));
        given(repository.existsByIdempotencyKey("k1")).willReturn(false);
        given(walletRepository.findByUserIdForUpdate(11L)).willReturn(Optional.of(receiver));
        given(walletRepository.findByUserIdForUpdate(12L)).willReturn(Optional.of(sender));
        persistReturnsEntity();
        willDoNothing().given(kafkaEventProducer).sendTransactionEvent(anyString(), any(Transaction.class));

        Transaction result = service.createTransaction(request(12L, 11L, "500", "k1"));

        assertEquals(new BigDecimal("500"), sender.getBalance());
        assertEquals(new BigDecimal("500"), receiver.getBalance());
        assertEquals("SUCCESS", result.getStatus());
        verify(walletRepository, times(2)).save(any(Wallet.class));

        ArgumentCaptor<LedgerEntry> entries = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(entries.capture());
        List<LedgerEntry> saved = entries.getAllValues();
        assertTrue(saved.stream().anyMatch(e -> "DEBIT".equals(e.getEntryType())
                && e.getAmount().compareTo(new BigDecimal("-500")) == 0));
        assertTrue(saved.stream().anyMatch(e -> "CREDIT".equals(e.getEntryType())
                && e.getAmount().compareTo(new BigDecimal("500")) == 0));
    }

    @Test
    void missingSenderWallet_throwsAccountNotFound() {
        given(repository.existsByIdempotencyKey("k1")).willReturn(false);
        given(walletRepository.findByUserIdForUpdate(11L)).willReturn(Optional.of(new Wallet(11L, BigDecimal.ZERO)));
        given(walletRepository.findByUserIdForUpdate(12L)).willReturn(Optional.empty());

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> service.createTransaction(request(12L, 11L, "500", "k1")));
        assertTrue(ex.getMessage().contains("Sender wallet not found for user 12"));
        verify(repository, never()).save(any());
    }

    @Test
    void missingReceiverWallet_throwsAccountNotFound() {
        given(repository.existsByIdempotencyKey("k1")).willReturn(false);
        given(walletRepository.findByUserIdForUpdate(11L)).willReturn(Optional.empty());

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> service.createTransaction(request(12L, 11L, "500", "k1")));
        assertTrue(ex.getMessage().contains("Receiver wallet not found for user 11"));
        verify(repository, never()).save(any());
    }

    @Test
    void insufficientFunds_throws_andDoesNotPersistTransaction() {
        given(repository.existsByIdempotencyKey("k1")).willReturn(false);
        given(walletRepository.findByUserIdForUpdate(11L)).willReturn(Optional.of(new Wallet(11L, BigDecimal.ZERO)));
        given(walletRepository.findByUserIdForUpdate(12L)).willReturn(Optional.of(new Wallet(12L, new BigDecimal("100"))));

        assertThrows(InsufficientFundsException.class,
                () -> service.createTransaction(request(12L, 11L, "500", "k1")));
        verify(repository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void duplicateIdempotencyKey_throws_andDoesNotTouchWallets() {
        given(repository.existsByIdempotencyKey("k1")).willReturn(true);

        assertThrows(DuplicateTransactionException.class,
                () -> service.createTransaction(request(12L, 11L, "500", "k1")));
        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(repository, never()).save(any());
    }

    @Test
    void selfTransfer_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createTransaction(request(12L, 12L, "500", "k1")));
    }

    @Test
    void nonPositiveAmount_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createTransaction(request(12L, 11L, "0", "k1")));
        assertThrows(IllegalArgumentException.class,
                () -> service.createTransaction(request(12L, 11L, "-5", "k1")));
    }

    @Test
    void missingAmount_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createTransaction(request(12L, 11L, null, "k1")));
    }

    @Test
    void locksBothWalletsInAscendingUserIdOrderToAvoidDeadlocks() {
        // Sender (12) has the higher id, so the lower id (receiver 11) must be locked first.
        Wallet sender = new Wallet(12L, new BigDecimal("1000"));
        Wallet receiver = new Wallet(11L, new BigDecimal("0"));
        given(repository.existsByIdempotencyKey("k1")).willReturn(false);
        given(walletRepository.findByUserIdForUpdate(11L)).willReturn(Optional.of(receiver));
        given(walletRepository.findByUserIdForUpdate(12L)).willReturn(Optional.of(sender));
        persistReturnsEntity();
        willDoNothing().given(kafkaEventProducer).sendTransactionEvent(anyString(), any(Transaction.class));

        service.createTransaction(request(12L, 11L, "500", "k1"));

        InOrder inOrder = inOrder(walletRepository);
        inOrder.verify(walletRepository).findByUserIdForUpdate(11L);
        inOrder.verify(walletRepository).findByUserIdForUpdate(12L);
    }

    @Test
    void kafkaFailure_doesNotBreakTheTransfer() {
        given(repository.existsByIdempotencyKey("k1")).willReturn(false);
        given(walletRepository.findByUserIdForUpdate(11L)).willReturn(Optional.of(new Wallet(11L, BigDecimal.ZERO)));
        given(walletRepository.findByUserIdForUpdate(12L)).willReturn(Optional.of(new Wallet(12L, new BigDecimal("1000"))));
        persistReturnsEntity();
        willThrow(new RuntimeException("kafka down"))
                .given(kafkaEventProducer).sendTransactionEvent(anyString(), any(Transaction.class));

        Transaction result = service.createTransaction(request(12L, 11L, "500", "k1"));

        assertEquals("SUCCESS", result.getStatus());
    }
}

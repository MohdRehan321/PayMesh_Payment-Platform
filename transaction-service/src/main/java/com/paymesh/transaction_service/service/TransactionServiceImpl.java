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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final KafkaEventProducer kafkaEventProducer;
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    public TransactionServiceImpl(TransactionRepository repository,
                                  WalletRepository walletRepository,
                                  LedgerEntryRepository ledgerEntryRepository,
                                  KafkaEventProducer kafkaEventProducer) {
        this.repository = repository;
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.kafkaEventProducer = kafkaEventProducer;
    }

    @Override
    @Transactional
    public Transaction createTransaction(TransferRequest request) {
        log.info("Entered createTransaction()");

        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }

        Long senderId = request.getSenderId();
        Long receiverId = request.getReceiverId();
        BigDecimal amount = request.getAmount();

        if (senderId == null || receiverId == null) {
            throw new IllegalArgumentException("Sender and receiver IDs are required");
        }
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Sender and receiver must be different users");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        if (repository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            throw new DuplicateTransactionException(
                    "Duplicate transaction detected for idempotency key: " + request.getIdempotencyKey());
        }

        Long lowerId = senderId < receiverId ? senderId : receiverId;
        Long higherId = senderId < receiverId ? receiverId : senderId;

        Wallet lowerWallet = walletRepository.findByUserIdForUpdate(lowerId)
                .orElseThrow(() -> new AccountNotFoundException(walletMissingMessage(lowerId, senderId)));
        Wallet higherWallet = walletRepository.findByUserIdForUpdate(higherId)
                .orElseThrow(() -> new AccountNotFoundException(walletMissingMessage(higherId, senderId)));

        Wallet senderWallet = lowerId.equals(senderId) ? lowerWallet : higherWallet;
        Wallet receiverWallet = lowerId.equals(receiverId) ? lowerWallet : higherWallet;

        // 3. Move the money and save updated balances
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        // 2. Check for sufficient funds
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in sender's wallet");
        }

        // 4. Record the overall Transaction
        Transaction transaction = new Transaction();
        transaction.setIdempotencyKey(request.getIdempotencyKey());
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("SUCCESS");
        Transaction savedTransaction = repository.save(transaction);

        // 5. Create Double-Entry Ledger Records
        LedgerEntry debit = new LedgerEntry(savedTransaction.getId(), senderId, amount.negate(), "DEBIT");
        LedgerEntry credit = new LedgerEntry(savedTransaction.getId(), receiverId, amount, "CREDIT");
        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);

        log.info("Saved transaction and ledger entries successfully (transactionId={})", savedTransaction.getId());

        sendEventSafely(savedTransaction);

        return savedTransaction;
    }

    private String walletMissingMessage(Long missingId, Long senderId) {
        return missingId.equals(senderId)
                ? "Sender wallet not found for user " + missingId
                : "Receiver wallet not found for user " + missingId;
    }

    private void sendEventSafely(Transaction savedTransaction) {

        try {
            String key = String.valueOf(savedTransaction.getId());
            kafkaEventProducer.sendTransactionEvent(key, savedTransaction);
            log.info("Kafka message sent for transactionId={}", savedTransaction.getId());
        } catch (Exception e) {
            log.error("Failed to send Kafka event for transactionId={}", savedTransaction.getId(), e);
        }

    }

    @Override
    public List<Transaction> getAllTransactions() {
        return repository.findAll();
    }

    @Override
    public List<Transaction> getTransactionsByUserId(Long userId) {
        return repository.findBySenderIdOrReceiverIdOrderByTimestampDesc(userId, userId);
    }
}
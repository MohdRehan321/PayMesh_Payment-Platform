package com.paymesh.transaction_service.service;


import com.paymesh.transaction_service.dto.TransferRequest;
import com.paymesh.transaction_service.entity.Transaction;

import java.util.List;


public interface TransactionService {

    Transaction createTransaction(TransferRequest request);
    List<Transaction> getAllTransactions();
}

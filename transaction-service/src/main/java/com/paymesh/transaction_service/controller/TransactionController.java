package com.paymesh.transaction_service.controller;

import com.paymesh.transaction_service.dto.DepositRequest;
import com.paymesh.transaction_service.dto.TransferRequest;
import com.paymesh.transaction_service.entity.Transaction;
import com.paymesh.transaction_service.service.TransactionService;
import com.paymesh.transaction_service.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/transactions/")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService service;
    private final WalletService walletService;

    public TransactionController(TransactionService service, WalletService walletService) {
        this.service = service;
        this.walletService = walletService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody TransferRequest request, Authentication authentication) {
        // Identity comes from the authenticated principal set by GatewayAuthFilter (validated
        // gateway secret), never from a client-supplied body/header value.
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        log.debug("Authenticated user {} requested transfer from {} to {}",
                authenticatedUserId, request.getSenderId(), request.getReceiverId());

        if (!authenticatedUserId.equals(request.getSenderId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Unauthorized: You cannot initiate a transfer from another user's account.");
        }

        Transaction created = service.createTransaction(request);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@Valid @RequestBody DepositRequest request, Authentication authentication) {
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(walletService.topUp(authenticatedUserId, request.getAmount()));
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTransactionsByUserId(
            @PathVariable("userId") Long userId,
            Authentication authentication) {

        Long authenticatedUserId =
                (Long) authentication.getPrincipal();

        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not authorized to access these transactions");
        }

        return ResponseEntity.ok(
                service.getTransactionsByUserId(authenticatedUserId)
        );
    }

}

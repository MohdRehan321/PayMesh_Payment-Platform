package com.paymesh.transaction_service.controller;

import com.paymesh.transaction_service.dto.TransferRequest;
import com.paymesh.transaction_service.entity.Transaction;
import com.paymesh.transaction_service.service.TransactionService;
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
    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }
    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody TransferRequest request, @RequestHeader("X-User-Id") Long authenticatedUserId) {
        System.out.println("Authenticated User ID: " + authenticatedUserId);
        System.out.println("Request Sender ID: " + request.getSenderId());
        System.out.println("Request Receiver ID: " + request.getReceiverId());
        if (!authenticatedUserId.equals(request.getSenderId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Unauthorized: You cannot initiate a transfer from another user's account.");
        }

        Transaction created = service.createTransaction(request);
        return ResponseEntity.ok(created);
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

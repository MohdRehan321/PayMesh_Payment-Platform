package com.paymesh.transaction_service.exception;

/**
 * Thrown when a transfer is submitted with an idempotency key that has already been used.
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String message) {
        super(message);
    }
}

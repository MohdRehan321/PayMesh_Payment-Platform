package com.paymesh.transaction_service.exception;

/**
 * Thrown when the sender does not have enough balance to complete the transfer.
 * Mapped to HTTP 422 by {@link GlobalExceptionHandler}.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}

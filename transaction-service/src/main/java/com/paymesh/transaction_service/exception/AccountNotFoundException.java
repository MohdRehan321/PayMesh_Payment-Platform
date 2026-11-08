package com.paymesh.transaction_service.exception;

/**
 * Thrown when the account/wallet required to perform a transfer does not exist.
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}

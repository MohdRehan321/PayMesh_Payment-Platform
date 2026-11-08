package com.paymesh.transaction_service.controller;

import com.paymesh.transaction_service.dto.TransferRequest;
import com.paymesh.transaction_service.entity.Transaction;
import com.paymesh.transaction_service.exception.AccountNotFoundException;
import com.paymesh.transaction_service.exception.DuplicateTransactionException;
import com.paymesh.transaction_service.exception.GlobalExceptionHandler;
import com.paymesh.transaction_service.exception.InsufficientFundsException;
import com.paymesh.transaction_service.service.TransactionService;
import com.paymesh.transaction_service.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the HTTP status mapping of the controller + {@link GlobalExceptionHandler} without
 * needing a database or a running Spring context.
 */
class TransactionControllerWebMvcTest {

    private TransactionService service;
    private WalletService walletService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(TransactionService.class);
        walletService = mock(WalletService.class);
        TransactionController controller = new TransactionController(service, walletService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    private Authentication auth(long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Transaction transaction() {
        Transaction t = new Transaction();
        t.setSenderId(12L);
        t.setReceiverId(11L);
        t.setAmount(new BigDecimal("500"));
        t.setStatus("SUCCESS");
        t.setTimestamp(LocalDateTime.now());
        t.setIdempotencyKey("k1");
        return t;
    }

    private String transferBody(long sender, long receiver, String amount) {
        return "{\"senderId\":" + sender + ",\"receiverId\":" + receiver
                + ",\"amount\":" + amount + ",\"idempotencyKey\":\"k1\"}";
    }

    @Test
    void create_returns403_whenSenderIsAnotherUser() throws Exception {
        mockMvc.perform(post("/api/transactions/create")
                        .principal(auth(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(99L, 11L, "500")))
                .andExpect(status().isForbidden());

        verify(service, never()).createTransaction(any());
    }

    @Test
    void create_returns200_forValidTransfer() throws Exception {
        given(service.createTransaction(any(TransferRequest.class))).willReturn(transaction());

        mockMvc.perform(post("/api/transactions/create")
                        .principal(auth(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(12L, 11L, "500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void create_returns404_whenSenderWalletMissing() throws Exception {
        given(service.createTransaction(any(TransferRequest.class)))
                .willThrow(new AccountNotFoundException("Sender wallet not found for user 12"));

        mockMvc.perform(post("/api/transactions/create")
                        .principal(auth(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(12L, 11L, "500")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sender wallet not found for user 12"));
    }

    @Test
    void create_returns422_whenInsufficientFunds() throws Exception {
        given(service.createTransaction(any(TransferRequest.class)))
                .willThrow(new InsufficientFundsException("Insufficient funds in sender's wallet"));

        mockMvc.perform(post("/api/transactions/create")
                        .principal(auth(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(12L, 11L, "500")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_returns409_whenDuplicateIdempotencyKey() throws Exception {
        given(service.createTransaction(any(TransferRequest.class)))
                .willThrow(new DuplicateTransactionException("Duplicate transaction detected"));

        mockMvc.perform(post("/api/transactions/create")
                        .principal(auth(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(12L, 11L, "500")))
                .andExpect(status().isConflict());
    }

    @Test
    void create_returns500_whenUnexpectedExceptionOccurs() throws Exception {
        given(service.createTransaction(any(TransferRequest.class)))
                .willThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/transactions/create")
                        .principal(auth(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(12L, 11L, "500")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void create_returns400_whenAmountIsMissing() throws Exception {
        mockMvc.perform(post("/api/transactions/create")
                        .principal(auth(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderId\":12,\"receiverId\":11,\"idempotencyKey\":\"k1\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).createTransaction(any());
    }

    @Test
    void create_returns400_whenAmountIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/transactions/create")
                        .principal(auth(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(12L, 11L, "0")))
                .andExpect(status().isBadRequest());

        verify(service, never()).createTransaction(any());
    }

    @Test
    void deposit_usesAuthenticatedUserAndReturns200() throws Exception {
        given(walletService.topUp(eq(12L), any(BigDecimal.class)))
                .willReturn(new com.paymesh.transaction_service.entity.Wallet(12L, new BigDecimal("1000")));

        mockMvc.perform(post("/api/transactions/deposit")
                        .principal(auth(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000}"))
                .andExpect(status().isOk());

        verify(walletService).topUp(eq(12L), eq(new BigDecimal("1000")));
    }

    @Test
    void deposit_returns400_whenAmountMissing() throws Exception {
        mockMvc.perform(post("/api/transactions/deposit")
                        .principal(auth(12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(walletService, never()).topUp(any(), any());
    }
}

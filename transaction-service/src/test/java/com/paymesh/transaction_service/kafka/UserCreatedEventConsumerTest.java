package com.paymesh.transaction_service.kafka;

import com.paymesh.transaction_service.dto.UserCreatedEvent;
import com.paymesh.transaction_service.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserCreatedEventConsumerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private UserCreatedEventConsumer consumer;

    @Test
    void onUserCreated_provisionsWalletForTheNewUser() {
        consumer.onUserCreated(new UserCreatedEvent(42L, "new@user.com", "New User"));

        verify(walletService).provisionWallet(42L);
    }

    @Test
    void onUserCreated_ignoresMalformedEvents() {
        consumer.onUserCreated(null);
        consumer.onUserCreated(new UserCreatedEvent());

        verify(walletService, never()).provisionWallet(org.mockito.ArgumentMatchers.any());
    }
}

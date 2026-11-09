package com.paymesh.transaction_service.kafka;

import com.paymesh.transaction_service.dto.UserCreatedEvent;
import com.paymesh.transaction_service.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserCreatedEventConsumer.class);

    private final WalletService walletService;

    public UserCreatedEventConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @KafkaListener(
            topics = "${paymesh.kafka.topics.user-created:user-created}",
            groupId = "${spring.kafka.consumer.group-id:transaction-group}"
    )
    public void onUserCreated(UserCreatedEvent event) {
        if (event == null || event.getUserId() == null) {
            log.warn("Ignoring malformed user-created event: {}", event);
            return;
        }
        walletService.provisionWallet(event.getUserId());
        log.info("Wallet provisioned for registered user userId={}", event.getUserId());
    }
}
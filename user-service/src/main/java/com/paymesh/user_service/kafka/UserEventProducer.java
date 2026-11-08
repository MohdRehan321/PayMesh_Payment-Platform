package com.paymesh.user_service.kafka;

import com.paymesh.user_service.dto.UserCreatedEvent;
import com.paymesh.user_service.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes the {@code user-created} event. Downstream services (transaction-service) use it
 * to provision the user's wallet, so registration never has to call them synchronously.
 */
@Component
public class UserEventProducer {

    private static final Logger log = LoggerFactory.getLogger(UserEventProducer.class);

    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    @Value("${paymesh.kafka.topics.user-created:user-created}")
    private String userCreatedTopic;

    public UserEventProducer(KafkaTemplate<String, UserCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Fire-and-forget publish. Registration must not fail just because Kafka is temporarily
     * unavailable; the event can be replayed/back-filled separately.
     */
    public void sendUserCreated(User user) {
        if (user == null || user.getId() == null) {
            log.warn("Skipping user-created event: user or user id is null");
            return;
        }
        try {
            UserCreatedEvent event = new UserCreatedEvent(user.getId(), user.getEmail(), user.getName());
            String key = String.valueOf(user.getId());
            kafkaTemplate.send(userCreatedTopic, key, event);
            log.info("Published user-created event for userId={} to topic={}", user.getId(), userCreatedTopic);
        } catch (Exception e) {
            log.error("Failed to publish user-created event for userId={}", user.getId(), e);
        }
    }
}

package com.paymesh.user_service.kafka;

import com.paymesh.user_service.dto.UserCreatedEvent;
import com.paymesh.user_service.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserEventProducerTest {

    @Mock
    private KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    private UserEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new UserEventProducer(kafkaTemplate);
        ReflectionTestUtils.setField(producer, "userCreatedTopic", "user-created");
    }

    private User user() {
        User u = new User();
        u.setId(5L);
        u.setEmail("u@example.com");
        u.setName("User");
        return u;
    }

    @Test
    void sendUserCreated_publishesEventKeyedByUserId() {
        producer.sendUserCreated(user());

        verify(kafkaTemplate).send(eq("user-created"), eq("5"),
                argThat(event -> event != null && Long.valueOf(5L).equals(event.getUserId())));
    }

    @Test
    void sendUserCreated_ignoresUsersWithoutId() {
        producer.sendUserCreated(null);
        producer.sendUserCreated(new User());

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void sendUserCreated_swallowsKafkaFailures() {
        given(kafkaTemplate.send(eq("user-created"), eq("5"), org.mockito.ArgumentMatchers.any(UserCreatedEvent.class)))
                .willThrow(new RuntimeException("kafka down"));

// Must not propagate: registration should succeed even if Kafka is unavailable.
        producer.sendUserCreated(user());
    }
}

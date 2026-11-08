package com.paymesh.user_service.dto;

/**
 * Published to Kafka when a new user registers. Consumed by the transaction-service to
 * provision the user's wallet.
 */
public class UserCreatedEvent {

    private Long userId;
    private String email;
    private String name;

    public UserCreatedEvent() {
    }

    public UserCreatedEvent(Long userId, String email, String name) {
        this.userId = userId;
        this.email = email;
        this.name = name;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "UserCreatedEvent{userId=" + userId + ", email='" + email + "'}";
    }
}

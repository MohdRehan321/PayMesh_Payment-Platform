package com.paymesh.transaction_service.dto;

import java.math.BigDecimal;

public class TransferRequest {

    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private String idempotencyKey;


    public TransferRequest() {
    }

    public TransferRequest(Long senderId, Long receiverId, BigDecimal amount,
                           String senderNameSnapshot, String receiverNameSnapshot) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
    }

    // Getters
    public Long getSenderId() {
        return senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    public String getIdempotencyKey() { return idempotencyKey; }

    // Setters
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

}

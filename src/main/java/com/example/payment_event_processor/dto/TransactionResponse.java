package com.example.payment_event_processor.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionResponse {

    private UUID transactionId;
    private UUID userId;
    private BigDecimal amount;
    private String type;
    private String status;
    private BigDecimal balance;

    public TransactionResponse() {
    }

    public TransactionResponse(
            UUID transactionId,
            UUID userId,
            BigDecimal amount,
            String type,
            String status,
            BigDecimal balance) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.balance = balance;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
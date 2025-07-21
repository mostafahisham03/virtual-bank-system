package com.vbank.bff_service.dto;

import lombok.Data;

@Data
public class Transaction {
    private String transactionId;
    private double amount;
    private String toAccountId;
    private String description;
    private String timestamp;
}

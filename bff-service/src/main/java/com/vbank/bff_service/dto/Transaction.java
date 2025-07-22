package com.vbank.bff_service.dto;

import lombok.Data;

@Data
public class Transaction {
    private String id;
    private double amount;
    private String toAccountId;
    private String fromAccountId;
    private TransactionStatus status;
    private String description;
    private String timestamp;
}

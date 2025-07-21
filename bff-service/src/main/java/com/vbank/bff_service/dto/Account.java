package com.vbank.bff_service.dto;

import java.util.List;

import lombok.Data;

@Data
public class Account {
    private String accountId;
    private String accountNumber;
    private String accountType;
    private double balance;
    private List<Transaction> transactions;
}

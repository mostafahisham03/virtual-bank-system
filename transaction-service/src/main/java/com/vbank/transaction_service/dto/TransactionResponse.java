package com.vbank.transaction_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.vbank.transaction_service.model.TransactionStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private UUID transactionId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private TransactionStatus status;
    private Instant timestamp;
    private String description;
}

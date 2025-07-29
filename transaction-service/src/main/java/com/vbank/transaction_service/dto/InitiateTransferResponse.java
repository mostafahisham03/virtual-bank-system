package com.vbank.transaction_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.vbank.transaction_service.model.TransactionStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitiateTransferResponse {
    private UUID transactionId;
    private TransactionStatus status;
    private Instant timestamp;
}

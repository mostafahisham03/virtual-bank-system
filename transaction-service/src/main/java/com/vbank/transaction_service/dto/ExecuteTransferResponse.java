package com.vbank.transaction_service.dto;

/**
 * Response DTO for executing a transfer.
 */
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.vbank.transaction_service.model.TransactionStatus;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteTransferResponse {
    private UUID transactionId;
    private TransactionStatus status;
    private Instant timestamp;
}

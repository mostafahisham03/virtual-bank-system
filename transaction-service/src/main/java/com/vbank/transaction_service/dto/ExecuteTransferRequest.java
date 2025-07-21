package com.vbank.transaction_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class ExecuteTransferRequest {
    @NotNull(message = "Transaction ID cannot be null")
    private UUID transactionId;
}

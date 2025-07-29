package com.vbank.account_service.dto;

import lombok.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotNull(message = "From account ID is required")
    private UUID fromAccountId;

    @NotNull(message = "To account ID is required")
    private UUID toAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than 0")
    private BigDecimal amount;
}

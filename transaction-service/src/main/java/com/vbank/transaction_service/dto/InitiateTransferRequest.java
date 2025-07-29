package com.vbank.transaction_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class InitiateTransferRequest {
    @NotNull
    @NotBlank(message = "From account ID cannot be blank")
    private UUID fromAccountId;
    @NotNull
    @NotBlank(message = "To account ID cannot be blank")
    private UUID toAccountId;
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    private String description;

}

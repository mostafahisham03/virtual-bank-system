package com.vbank.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class AccountBalanceUpdateDTO {
    @NotNull
    @NotBlank(message = "From account ID cannot be null")
    private UUID fromAccountId;
    @NotNull
    @NotBlank(message = "To account ID cannot be null")
    private UUID toAccountId;
    @NotNull
    @NotBlank(message = "Amount cannot be null")
    private BigDecimal Amount;

}

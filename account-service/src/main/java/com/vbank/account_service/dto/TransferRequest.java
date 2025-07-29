package com.vbank.account_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotNull
    @NotBlank(message = "From account ID cannot be blank")
    private UUID fromAccountId;
    @NotNull
    @NotBlank(message = "To account ID cannot be blank")
    private UUID toAccountId;
    @NotNull
    @NotBlank(message = "Amount cannot be blank")
    private BigDecimal amount;
}

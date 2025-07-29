package com.vbank.account_service.dto;

import com.vbank.account_service.model.AccountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {
    @NotNull
    @NotBlank(message = "User ID cannot be blank")
    private UUID userId;
    @NotNull
    @NotBlank(message = "Account type cannot be blank")
    private AccountType accountType;
    @NotNull
    @NotBlank(message = "Initial balance cannot be blank")
    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance must be non-negative")
    private BigDecimal initialBalance;
}

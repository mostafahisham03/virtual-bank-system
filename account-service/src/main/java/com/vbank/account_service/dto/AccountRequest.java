package com.vbank.account_service.dto;

import com.vbank.account_service.model.AccountType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {
    private UUID userId;
    private AccountType accountType;
    private BigDecimal initialBalance;
}

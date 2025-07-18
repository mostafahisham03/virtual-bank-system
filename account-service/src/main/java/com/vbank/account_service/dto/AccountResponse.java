package com.vbank.account_service.dto;

import com.vbank.account_service.model.AccountStatus;
import com.vbank.account_service.model.AccountType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
    private UUID accountId;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
}

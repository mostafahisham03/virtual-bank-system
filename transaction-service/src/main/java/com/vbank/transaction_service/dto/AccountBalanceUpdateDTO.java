package com.vbank.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class AccountBalanceUpdateDTO {
    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal Amount;

}

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
    private UUID toAccountId;
    private UUID fromAccountId;
    private BigDecimal Amount;

}

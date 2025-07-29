package com.vbank.transaction_service.service; // Corrected package name

import com.vbank.transaction_service.dto.ExecuteTransferResponse;
import com.vbank.transaction_service.dto.InitiateTransferResponse;
import com.vbank.transaction_service.dto.TransactionResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransactionService {
    InitiateTransferResponse initiateTransfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount,
            String description);

    ExecuteTransferResponse executeTransfer(UUID transactionId);

    List<TransactionResponse> getTransactionsForAccount(UUID accountId);
}
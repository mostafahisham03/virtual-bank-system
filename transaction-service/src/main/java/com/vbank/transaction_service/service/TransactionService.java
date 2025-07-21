package com.vbank.transaction_service.service; // Corrected package name

import com.vbank.transaction_service.model.Transaction; // Corrected import
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransactionService {
    Transaction initiateTransfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String description);

    Transaction executeTransfer(UUID transactionId);

    List<Transaction> getTransactionsForAccount(UUID accountId);
}
package com.vbank.transaction_service.repository; // Corrected package name

import com.vbank.transaction_service.model.Transaction; // Corrected import
import com.vbank.transaction_service.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByFromAccountIdOrToAccountIdOrderByTimestampDesc(UUID accountId1, UUID accountId2);
    Optional<Transaction> findByIdAndStatus(UUID id, TransactionStatus status);
}
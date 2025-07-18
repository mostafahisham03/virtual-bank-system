package com.vbank.account_service.repository;

import com.vbank.account_service.model.Account;
import com.vbank.account_service.model.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByUserId(UUID userId);

    Optional<Account> findById(UUID id);

    List<Account> findByStatusAndUpdatedAtBefore(AccountStatus status, LocalDateTime cutoff);
}

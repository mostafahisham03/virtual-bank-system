package com.vbank.account_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vbank.account_service.model.Account;
import com.vbank.account_service.model.AccountStatus;
import com.vbank.account_service.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountStatusScheduler {

    private final AccountRepository repository;

    @Scheduled(cron = "0 0 * * * *") // every hour
    public void inactivateIdleAccounts() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<Account> staleAccounts = repository.findByStatusAndUpdatedAtBefore(AccountStatus.ACTIVE, threshold);

        for (Account acc : staleAccounts) {
            acc.setStatus(AccountStatus.INACTIVE);
            repository.save(acc);
        }
    }
}

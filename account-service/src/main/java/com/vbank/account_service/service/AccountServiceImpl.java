package com.vbank.account_service.service;

import com.vbank.account_service.dto.*;
import com.vbank.account_service.model.*;
import com.vbank.account_service.exception.AccountNotFoundException;
import com.vbank.account_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public AccountResponse createAccount(AccountRequest request) {
        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0 ||
                (!request.getAccountType().equals("SAVINGS") &&
                        !request.getAccountType().equals("CHECKING"))) {
            throw new IllegalArgumentException("Invalid account type or initial balance.");
        }

        Account account = Account.builder()
                .userId(request.getUserId())
                .accountNumber(generateAccountNumber())
                .accountType(request.getAccountType())
                .balance(request.getInitialBalance())
                .status(AccountStatus.ACTIVE)
                .build();

        accountRepository.save(account);

        return mapToResponse(account);
    }

    @Override
    public AccountResponse getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return mapToResponse(account);
    }

    @Override
    public List<AccountResponse> getAccountsByUserId(UUID userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts found for user ID " + userId);
        }
        return accounts.stream().map(this::mapToResponse).collect(toList());
    }

    @Override
    @Transactional
    public void transferFunds(TransferRequest request) {
        Account from = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getFromAccountId()));
        Account to = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getToAccountId()));

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance.");
        }

        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));

        from.setStatus(AccountStatus.ACTIVE);

        to.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(from);
        accountRepository.save(to);
    }

    private String generateAccountNumber() {
        return String.valueOf(System.currentTimeMillis()).substring(3, 13);
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }
}

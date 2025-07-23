package com.vbank.account_service.service;

import com.vbank.account_service.config.KafkaLogger;
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
    private final KafkaLogger kafkaLogger;

    @Override
    public AccountResponse createAccount(AccountRequest request) {
        kafkaLogger.sendLog(request.toString(), "Request");

        AccountType type = request.getAccountType();
        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0 ||
                (type != AccountType.SAVINGS && type != AccountType.CHECKING)) {
            kafkaLogger.sendLog("Invalid account type or initial balance: " + request, "Response");
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

        kafkaLogger.sendLog("Account created: " + account.getId(), "Response");

        return mapToResponse(account);
    }

    @Override
    public AccountResponse getAccountById(UUID accountId) {
        kafkaLogger.sendLog("Fetching account by ID: " + accountId, "Request");

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    kafkaLogger.sendLog("Account not found: " + accountId, "Response");
                    return new AccountNotFoundException("Account with ID " + accountId + " not found.");
                });
        kafkaLogger.sendLog("Account found: " + account.getId(), "Response");
        return mapToResponse(account);
    }

    @Override
    public List<AccountResponse> getAccountsByUserId(UUID userId) {
        kafkaLogger.sendLog("Fetching accounts for user ID: " + userId, "Request");
        List<Account> accounts = accountRepository.findByUserId(userId);
        if (accounts.isEmpty()) {
            kafkaLogger.sendLog("No accounts found for user ID: " + userId, "Response");
            throw new AccountNotFoundException("No accounts found for user ID " + userId);
        }
        kafkaLogger.sendLog("Accounts found for user ID: " + userId, "Response");
        return accounts.stream().map(this::mapToResponse).collect(toList());
    }

    @Override
    @Transactional
    public void transferFunds(TransferRequest request) {
        kafkaLogger.sendLog("Transfer request: " + request.toString(), "Request");
        Account from = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getFromAccountId()));
        Account to = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getToAccountId()));

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            kafkaLogger.sendLog("Insufficient balance in source account: " + from.getBalance() +
                    ", required: " + request.getAmount(), "Response");
            throw new IllegalArgumentException("Insufficient balance.");
        }

        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));

        from.setStatus(AccountStatus.ACTIVE);

        to.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(from);
        accountRepository.save(to);
        kafkaLogger.sendLog("Transfer successful from " + from.getId() + " to " + to.getId(), "Response");
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

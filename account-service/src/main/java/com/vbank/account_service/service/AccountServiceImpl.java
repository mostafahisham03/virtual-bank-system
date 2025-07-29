package com.vbank.account_service.service;

import com.vbank.account_service.config.KafkaLogger;
import com.vbank.account_service.dto.*;
import com.vbank.account_service.model.*;
import com.vbank.account_service.exception.BadRequestException;
import com.vbank.account_service.exception.ResourceNotFoundException;
import com.vbank.account_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final KafkaLogger kafkaLogger;
    private final WebClient.Builder webClientBuilder;

    @Override
    public AccountResponse createAccount(AccountRequest request) {
        kafkaLogger.sendLog(request.toString(), "Request");
        try {
            String userServiceUrl = "http://localhost:8081/users/" + request.getUserId() + "/profile";
            webClientBuilder.build()
                    .get()
                    .uri(userServiceUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new ResourceNotFoundException();
        }

        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException();
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
                .orElseThrow(ResourceNotFoundException::new);

        kafkaLogger.sendLog("Account found: " + account.getId(), "Response");
        return mapToResponse(account);
    }

    @Override
    public List<AccountResponse> getAccountsByUserId(UUID userId) {
        kafkaLogger.sendLog("Fetching accounts for user ID: " + userId, "Request");

        if (userId == null) {
            throw new BadRequestException();
        }

        try {
            String userServiceUrl = "http://localhost:8081/users/" + userId + "/profile";
            webClientBuilder.build()
                    .get()
                    .uri(userServiceUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new ResourceNotFoundException();
        }

        List<Account> accounts = accountRepository.findByUserId(userId);
        if (accounts.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        return accounts.stream().map(this::mapToResponse).collect(toList());
    }

    @Override
    @Transactional
    public TransferResponse transferFunds(TransferRequest request) {
        kafkaLogger.sendLog("Transfer request: " + request.toString(), "Request");

        Account from = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(ResourceNotFoundException::new);
        Account to = accountRepository.findById(request.getToAccountId())
                .orElseThrow(ResourceNotFoundException::new);

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException();
        }

        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));
        from.setStatus(AccountStatus.ACTIVE);
        to.setStatus(AccountStatus.ACTIVE);

        accountRepository.save(from);
        accountRepository.save(to);

        kafkaLogger.sendLog("Transfer successful from " + from.getId() + " to " + to.getId(), "Response");
        return new TransferResponse("Transfer completed successfully.");
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

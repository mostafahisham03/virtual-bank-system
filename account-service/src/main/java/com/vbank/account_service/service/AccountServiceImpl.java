package com.vbank.account_service.service;

import com.vbank.account_service.config.KafkaLogger;
import com.vbank.account_service.dto.*;
import com.vbank.account_service.exception.BadRequestException;
import com.vbank.account_service.exception.ResourceNotFoundException;
import com.vbank.account_service.model.*;
import com.vbank.account_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final KafkaLogger kafkaLogger;
    private final WebClient.Builder webClientBuilder;


    @Value("${services.user.base-url:http://localhost:8081}")
    private String userServiceBaseUrl;

    @Override
    public AccountResponse createAccount(AccountRequest request) {
        // Log request (as a simple String per your current logger)
        kafkaLogger.sendLog(request.toString(), "Request");
        assertUserExists(request.getUserId());

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

        AccountResponse response = mapToResponse(account);
        kafkaLogger.sendLog(response.toString(), "Response");
        return response;
    }

    @Override
    public AccountResponse getAccountById(UUID accountId) {
        kafkaLogger.sendLog(Map.of("accountId", accountId).toString(), "Request");

        Account account = accountRepository.findById(accountId)
                .orElseThrow(ResourceNotFoundException::new);

        AccountResponse response = mapToResponse(account);
        kafkaLogger.sendLog(response.toString(), "Response");
        return response;
    }

    @Override
    public List<AccountResponse> getAccountsByUserId(UUID userId) {
        kafkaLogger.sendLog(Map.of("userId", userId).toString(), "Request");
        assertUserExists(userId);

        List<Account> accounts = accountRepository.findByUserId(userId);
        if (accounts.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        List<AccountResponse> response = accounts.stream()
                .map(this::mapToResponse)
                .collect(toList());

        kafkaLogger.sendLog(Map.of("userId", userId, "count", response.size()).toString(), "Response");
        return response;
    }

    @Override
    @Transactional
    public TransferResponse transferFunds(TransferRequest request) {
        kafkaLogger.sendLog(request.toString(), "Request");

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

        TransferResponse response = new TransferResponse("Transfer completed successfully.");
        kafkaLogger.sendLog(Map.of(
                "from", from.getId(),
                "to", to.getId(),
                "amount", request.getAmount()
        ).toString(), "Response");
        return response;
    }

    // ===== Helpers =====

    /**
     * Calls user-service: GET /users/{userId}/profile
     * - If 2xx: user exists
     * - If 404: throw ResourceNotFoundException()
     * - Otherwise: let WebClient raise an exception (handled globally)
     */
    private void assertUserExists(UUID userId) {
        String url = userServiceBaseUrl + "/users/{id}/profile";
        webClientBuilder.build()
                .get()
                .uri(url, userId)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return Mono.empty(); // OK -> user exists
                    }
                    if (clientResponse.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new ResourceNotFoundException());
                    }
                    // other statuses: propagate
                    return clientResponse.createException().flatMap(Mono::error);
                })
                .block();
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

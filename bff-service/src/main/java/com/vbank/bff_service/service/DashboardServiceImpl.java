package com.vbank.bff_service.service;

import com.vbank.bff_service.dto.*;
import com.vbank.bff_service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final WebClient.Builder webClientBuilder;

    @Override
    public DashboardResponse getDashboard(String userId) {
        WebClient userClient = webClientBuilder.baseUrl("http://USER-SERVICE").build();
        WebClient accountClient = webClientBuilder.baseUrl("http://ACCOUNT-SERVICE").build();
        WebClient transactionClient = webClientBuilder.baseUrl("http://TRANSACTION-SERVICE").build();

        UserProfile user = userClient.get()
                .uri("/users/{userId}/profile", userId)
                .retrieve()
                .bodyToMono(UserProfile.class)
                .block();

        List<Account> accounts = accountClient.get()
                .uri("accounts/users/{userId}", userId)
                .retrieve()
                .bodyToFlux(Account.class)
                .collectList()
                .block();

        if (accounts != null) {
            for (Account account : accounts) {
                List<Transaction> transactions = transactionClient.get()
                        .uri("/accounts/{accountId}/transactions", account.getAccountId())
                        .retrieve()
                        .bodyToFlux(Transaction.class)
                        .collectList()
                        .onErrorReturn(Collections.emptyList())
                        .block();
                account.setTransactions(transactions);
            }
        }

        return DashboardResponse.builder()
                .userId(userId)
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .accounts(accounts)
                .build();
    }
}

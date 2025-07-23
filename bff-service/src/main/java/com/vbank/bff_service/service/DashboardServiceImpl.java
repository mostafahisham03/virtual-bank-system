package com.vbank.bff_service.service;

import com.vbank.bff_service.config.KafkaLogger;
import com.vbank.bff_service.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

        private final WebClient.Builder webClientBuilder;
        private final KafkaLogger kafkaLogger;

        @Value("${user.service.url}")
        private String userServiceUrl;

        @Value("${account.service.url}")
        private String accountServiceUrl;

        @Value("${transaction.service.url}")
        private String transactionServiceUrl;

        @Override
        public Mono<DashboardResponse> getDashboard(String userId) {
                kafkaLogger.sendLog("Fetching dashboard for user ID: " + userId, "Request");

                // Create WebClients with proper URLs
                WebClient userClient = webClientBuilder.baseUrl(userServiceUrl).build();
                WebClient accountClient = webClientBuilder.baseUrl(accountServiceUrl).build();
                WebClient transactionClient = webClientBuilder.baseUrl(transactionServiceUrl).build();

                // Fetch user profile and accounts in parallel
                Mono<UserProfile> userProfileMono = userClient.get()
                                .uri("/users/{userId}/profile", userId)
                                .retrieve()
                                .bodyToMono(UserProfile.class)
                                .timeout(Duration.ofSeconds(10))
                                .onErrorResume(ex -> {
                                        kafkaLogger.sendLog("Error fetching user profile for userId: " + userId,
                                                        "Response");
                                        log.error("Error fetching user profile for userId: {}", userId, ex);
                                        // return a mono error 404 user profile not found
                                        return Mono.error(new ResponseStatusException(
                                                        HttpStatus.NOT_FOUND,
                                                        "User with ID " + userId + " not found."));
                                });

                Mono<List<Account>> accountsMono = accountClient.get()
                                .uri("/accounts/users/{userId}", userId) // Fixed URI
                                .retrieve()
                                .bodyToFlux(Account.class)
                                .timeout(Duration.ofSeconds(10))
                                .collectList()
                                .onErrorResume(ex -> {
                                        kafkaLogger.sendLog("Error fetching accounts for userId: " + userId,
                                                        "Response");
                                        log.error("Error fetching accounts for userId: {}", userId, ex);
                                        return Mono.just(Collections.emptyList());
                                });

                // Combine user profile and accounts, then fetch transactions
                return Mono.zip(userProfileMono, accountsMono)
                                .flatMap(tuple -> {
                                        UserProfile userProfile = tuple.getT1();
                                        List<Account> accounts = tuple.getT2();

                                        // Fetch transactions for each account
                                        List<Mono<Account>> accountWithTransactions = accounts.stream()
                                                        .map(account -> fetchTransactionsForAccount(transactionClient,
                                                                        account))
                                                        .toList();

                                        return Mono.zip(accountWithTransactions, objects -> Arrays.asList(objects))
                                                        .cast(List.class)
                                                        .map(accountsWithTransactions -> buildDashboardResponse(
                                                                        userProfile,
                                                                        accountsWithTransactions, userId));
                                })
                                .doOnSuccess(response -> kafkaLogger.sendLog(
                                                "Dashboard fetched successfully for user ID: " + userId,
                                                "Response"))
                                .doOnError(ex -> kafkaLogger.sendLog(
                                                "Error fetching dashboard for user ID: " + userId + ", Error: "
                                                                + ex.getMessage(),
                                                "Error"))
                                .onErrorResume(ex -> {
                                        log.error("Error fetching dashboard for userId: {}", userId, ex);
                                        return Mono.error(new ResponseStatusException(
                                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                                        "Failed to retrieve dashboard data due to an issue with downstream services."));
                                });

        }

        private Mono<Account> fetchTransactionsForAccount(WebClient transactionClient, Account account) {
                return transactionClient.get()
                                .uri("/transactions/accounts/{accountId}", account.getAccountId())
                                .retrieve()
                                .bodyToFlux(Transaction.class)
                                .timeout(Duration.ofSeconds(10))
                                .collectList()
                                .map(transactions -> {
                                        account.setTransactions(transactions);
                                        return account;
                                })
                                .onErrorResume(ex -> {
                                        kafkaLogger.sendLog("Error fetching transactions for accountId: "
                                                        + account.getAccountId(), "Response");
                                        log.error("Error fetching transactions for accountId: {}",
                                                        account.getAccountId(), ex);
                                        account.setTransactions(Collections.emptyList());
                                        return Mono.just(account);
                                });
        }

        // private UserProfile createDefaultUserProfile(String userId) {
        // return UserProfile.builder()
        // .userId(userId)
        // .username("john.doe") // Consider removing or fetching from cache
        // .email("johndoe@email.com")
        // .firstName("John")
        // .lastName("Doe")
        // .build();
        // }

        private DashboardResponse buildDashboardResponse(UserProfile userProfile, List<Account> accounts,
                        String userId) {
                return DashboardResponse.builder()
                                .userId(userId)
                                .username(userProfile.getUsername())
                                .email(userProfile.getEmail())
                                .firstName(userProfile.getFirstName())
                                .lastName(userProfile.getLastName())
                                .accounts(accounts)
                                .build();
        }
}
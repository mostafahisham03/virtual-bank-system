package com.vbank.transaction_service.service;

import com.vbank.transaction_service.model.Transaction;
import com.vbank.transaction_service.model.TransactionStatus;
import com.vbank.transaction_service.repository.TransactionRepository;
import com.vbank.transaction_service.config.KafkaLogger;
import com.vbank.transaction_service.dto.AccountBalanceUpdateDTO;
import com.vbank.transaction_service.dto.ExecuteTransferResponse;
import com.vbank.transaction_service.dto.InitiateTransferResponse;
import com.vbank.transaction_service.dto.TransactionResponse;
import com.vbank.transaction_service.exception.BadRequestException;
import com.vbank.transaction_service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

        @Data
        public static class AccountDetails {
                private UUID accountId;
                private String accountNumber;
                private String accountType;
                private BigDecimal balance;
                private String status;
        }

        private final TransactionRepository transactionRepository;
        private final WebClient.Builder webClientBuilder;
        private final KafkaLogger kafkaLogger;

        @Value("${logging.kafka.topic:vbank-logs}")
        private String loggingKafkaTopic;

        @Value("${account.service.url:http://localhost:8082}")
        private String accountServiceUrl;

        @Override
        @Transactional
        public InitiateTransferResponse initiateTransfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount,
                        String description) {
                kafkaLogger.sendLog("Initiating transfer from " + fromAccountId + " to " + toAccountId
                                + " for amount: " + amount + " with description: " + description, "Request");

                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        kafkaLogger.sendLog("Transfer amount must be positive: " + amount, "Response");
                        throw new BadRequestException("Transfer amount must be positive.");
                }

                if (fromAccountId.equals(toAccountId)) {
                        kafkaLogger.sendLog("Cannot transfer to the same account: " + fromAccountId, "Response");
                        throw new BadRequestException("Cannot transfer to the same account.");
                }

                // Validate accounts existence and balance by fetching account details
                try {
                        WebClient webClient = webClientBuilder.baseUrl(accountServiceUrl).build();

                        kafkaLogger.sendLog("Validating accounts and balance for transfer from " + fromAccountId +
                                        " to " + toAccountId + " for amount: " + amount, "Request");

                        // Validate source account exists and has sufficient balance
                        AccountDetails fromAccount = webClient.get()
                                        .uri("/accounts/{accountId}", fromAccountId)
                                        .retrieve()
                                        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                                        clientResponse -> clientResponse.bodyToMono(String.class)
                                                                        .flatMap(errorBody -> {
                                                                                kafkaLogger.sendLog(
                                                                                                "Failed to fetch source account ("
                                                                                                                + clientResponse.statusCode()
                                                                                                                + "): "
                                                                                                                + errorBody,
                                                                                                "Response");
                                                                                System.err.println(
                                                                                                "Failed to fetch source account ("
                                                                                                                + clientResponse.statusCode()
                                                                                                                + "): "
                                                                                                                + errorBody);

                                                                                int statusCodeValue = clientResponse
                                                                                                .statusCode().value();
                                                                                HttpStatus httpStatus = HttpStatus
                                                                                                .resolve(statusCodeValue);
                                                                                String reasonPhrase = (httpStatus != null)
                                                                                                ? httpStatus.getReasonPhrase()
                                                                                                : "Unknown Error";
                                                                                kafkaLogger.sendLog(
                                                                                                "Source account validation failed: "
                                                                                                                + errorBody,
                                                                                                "Response");
                                                                                return Mono.error(
                                                                                                new WebClientResponseException(
                                                                                                                "Source account validation failed: "
                                                                                                                                + errorBody,
                                                                                                                statusCodeValue,
                                                                                                                reasonPhrase,
                                                                                                                null,
                                                                                                                null,
                                                                                                                null));
                                                                        }))
                                        .bodyToMono(AccountDetails.class)
                                        .block();

                        // Validate destination account exists
                        AccountDetails toAccount = webClient.get()
                                        .uri("/accounts/{accountId}", toAccountId)
                                        .retrieve()
                                        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                                        clientResponse -> clientResponse.bodyToMono(String.class)
                                                                        .flatMap(errorBody -> {
                                                                                System.err.println(
                                                                                                "Failed to fetch destination account ("
                                                                                                                + clientResponse.statusCode()
                                                                                                                + "): "
                                                                                                                + errorBody);

                                                                                int statusCodeValue = clientResponse
                                                                                                .statusCode().value();
                                                                                HttpStatus httpStatus = HttpStatus
                                                                                                .resolve(statusCodeValue);
                                                                                String reasonPhrase = (httpStatus != null)
                                                                                                ? httpStatus.getReasonPhrase()
                                                                                                : "Unknown Error";
                                                                                kafkaLogger.sendLog(
                                                                                                "Failed to fetch destination account ("
                                                                                                                + clientResponse.statusCode()
                                                                                                                + "): "
                                                                                                                + errorBody,
                                                                                                "Response");
                                                                                return Mono.error(
                                                                                                new WebClientResponseException(
                                                                                                                "Destination account validation failed: "
                                                                                                                                + errorBody,
                                                                                                                statusCodeValue,
                                                                                                                reasonPhrase,
                                                                                                                null,
                                                                                                                null,
                                                                                                                null));
                                                                        }))
                                        .bodyToMono(AccountDetails.class)
                                        .block();

                        // Validate sufficient balance
                        if (fromAccount.getBalance().compareTo(amount) < 0) {
                                kafkaLogger.sendLog("Insufficient balance in source account: " +
                                                fromAccount.getBalance() + ", required: " + amount, "Response");
                                throw new BadRequestException("Insufficient balance. Available: " +
                                                fromAccount.getBalance() + ", Required: " + amount);
                        }

                        kafkaLogger.sendLog("Account validation successful - Source balance: " +
                                        fromAccount.getBalance() + ", Transfer amount: " + amount, "Response");

                } catch (WebClientResponseException e) {
                        kafkaLogger.sendLog("Account validation failed: " + e.getMessage(), "Response");
                        System.err.println("Account validation failed: " + e.getMessage());
                        throw new BadRequestException("Account validation failed: " + e.getMessage());
                } catch (Exception e) {
                        kafkaLogger.sendLog(
                                        "Unexpected error during transfer validation: " + e.getMessage(), "Response");
                        throw new BadRequestException("Unexpected error during validation: " + e.getMessage());

                }

                // Create transaction record in INITIATED status after successful validation
                Transaction transaction = Transaction.builder()
                                .fromAccountId(fromAccountId)
                                .toAccountId(toAccountId)
                                .amount(amount)
                                .description(description)
                                .status(TransactionStatus.INITIATED)
                                .timestamp(Instant.now())
                                .build();

                Transaction savedTransaction = transactionRepository.save(transaction);
                kafkaLogger.sendLog("Transaction initiated: " + savedTransaction.getId(), "Response");
                InitiateTransferResponse response = new InitiateTransferResponse(
                                savedTransaction.getId(),
                                savedTransaction.getStatus(),
                                savedTransaction.getTimestamp());
                return response;
        }

        @Override
        @Transactional
        public ExecuteTransferResponse executeTransfer(UUID transactionId) {
                kafkaLogger.sendLog("Executing transfer for transaction ID: " +
                                transactionId, "Request");

                Transaction transaction = transactionRepository
                                .findByIdAndStatus(transactionId, TransactionStatus.INITIATED)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction not found or not in 'INITIATED' status with ID: "
                                                                + transactionId));

                try {
                        // Perform the actual fund transfer
                        WebClient webClient = webClientBuilder.baseUrl(accountServiceUrl).build();

                        AccountBalanceUpdateDTO updateDto = new AccountBalanceUpdateDTO(
                                        transaction.getFromAccountId(),
                                        transaction.getToAccountId(),
                                        transaction.getAmount());

                        kafkaLogger.sendLog("Executing fund transfer to Account Service: " +
                                        updateDto.toString(),
                                        "Request");

                        webClient.put()
                                        .uri("/accounts/transfer")
                                        .bodyValue(updateDto)
                                        .retrieve()
                                        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                                        clientResponse -> clientResponse.bodyToMono(String.class)
                                                                        .flatMap(errorBody -> {
                                                                                System.err.println(
                                                                                                "Account Service transfer failed ("
                                                                                                                + clientResponse.statusCode()
                                                                                                                + "): "
                                                                                                                + errorBody);

                                                                                int statusCodeValue = clientResponse
                                                                                                .statusCode().value();
                                                                                HttpStatus httpStatus = HttpStatus
                                                                                                .resolve(statusCodeValue);
                                                                                String reasonPhrase = (httpStatus != null)
                                                                                                ? httpStatus.getReasonPhrase()
                                                                                                : "Unknown Error";
                                                                                markTransactionAsFailed(transaction,
                                                                                                reasonPhrase);
                                                                                return Mono.error(
                                                                                                new WebClientResponseException(
                                                                                                                "Fund transfer failed: "
                                                                                                                                + errorBody,
                                                                                                                statusCodeValue,
                                                                                                                reasonPhrase,
                                                                                                                null,
                                                                                                                null,
                                                                                                                null));
                                                                        }))
                                        .bodyToMono(Void.class)
                                        .block();

                        // If we reach here, the transfer was successful
                        transaction.setStatus(TransactionStatus.SUCCESS);
                        transaction.setTimestamp(Instant.now());
                        transactionRepository.save(transaction);

                        kafkaLogger.sendLog("Transfer executed successfully for transaction ID: " +
                                        transactionId,
                                        "Response");
                        ExecuteTransferResponse response = new ExecuteTransferResponse(
                                        transaction.getId(),
                                        transaction.getStatus(),
                                        transaction.getTimestamp());

                        return response;

                } catch (WebClientResponseException e) {
                        // Handle account service errors
                        transaction.setStatus(TransactionStatus.FAILED);
                        transaction.setTimestamp(Instant.now());
                        transactionRepository.save(transaction);
                        kafkaLogger.sendLog(
                                        "Transfer failed for transaction ID: " + transactionId + " - " +
                                                        e.getMessage(),
                                        "Response");
                        throw new BadRequestException(
                                        "Transfer execution failed: " + e.getMessage());

                } catch (Exception e) {
                        // Handle unexpected errors
                        transaction.setStatus(TransactionStatus.FAILED);
                        transaction.setTimestamp(Instant.now());
                        transactionRepository.save(transaction);
                        System.err.println("An unexpected error occurred during transaction execution for ID "
                                        + transactionId + ": " + e.getMessage());
                        kafkaLogger.sendLog(
                                        "Unexpected error during transfer execution for transaction ID: "
                                                        + transactionId + " - " + e.getMessage(),
                                        "Response");
                        throw new BadRequestException(
                                        "Unexpected error during transfer execution: " + e.getMessage());
                }
        }

        @Override
        public List<TransactionResponse> getTransactionsForAccount(UUID accountId) {
                kafkaLogger.sendLog("Fetching transactions for account ID: " + accountId,
                                "Request");
                List<Transaction> transactions = transactionRepository
                                .findByFromAccountIdOrToAccountIdOrderByTimestampDesc(accountId, accountId);
                kafkaLogger.sendLog("Fetched " + transactions.size() + " transactions for account ID: " + accountId,
                                "Response");
                if (transactions.isEmpty()) {
                        kafkaLogger.sendLog("No transactions found for account ID: " + accountId,
                                        "Response");
                        throw new ResourceNotFoundException("No transactions found for account ID: " + accountId);
                }
                return transactions.stream()
                                .map(transaction -> new TransactionResponse(
                                                transaction.getId(),
                                                transaction.getFromAccountId(),
                                                transaction.getToAccountId(),
                                                transaction.getAmount(),
                                                transaction.getStatus(),
                                                transaction.getTimestamp(),
                                                transaction.getDescription()))
                                .toList();

        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void markTransactionAsFailed(Transaction transaction, String reason) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setTimestamp(Instant.now());
                transactionRepository.save(transaction);
                kafkaLogger.sendLog("Transaction marked as FAILED: " + transaction.getId() + " - Reason: " + reason,
                                "Response");
        }

}
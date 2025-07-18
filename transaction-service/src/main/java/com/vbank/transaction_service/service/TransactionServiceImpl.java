package com.vbank.transaction_service.service; // Corrected package name

import com.vbank.transaction_service.model.Transaction; // Corrected import
import com.vbank.transaction_service.model.TransactionStatus; // Corrected import
import com.vbank.transaction_service.repository.TransactionRepository; // Corrected import
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${logging.kafka.topic:transaction-logs}")
    private String loggingKafkaTopic;

    @Value("${account.service.url:http://localhost:8081}") // Assuming Account Service runs on 8081
    private String accountServiceUrl;

    @Override
    @Transactional
    public Transaction initiateTransfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }

        Transaction transaction = Transaction.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .description(description)
                .status(TransactionStatus.INITIATED)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        logToKafka("Request", savedTransaction);
        return savedTransaction;
    }

    @Override
    @Transactional
    public Transaction executeTransfer(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));

        if (!transaction.getStatus().equals(TransactionStatus.INITIATED)) {
            throw new IllegalStateException("Transaction is not in INITIATED status and cannot be executed. Current status: " + transaction.getStatus());
        }

        try {
            WebClient webClient = webClientBuilder.baseUrl(accountServiceUrl).build();

            webClient.put()
                    .uri("/accounts/transfer")
                    .bodyValue(Map.of(
                            "fromAccountId", transaction.getFromAccountId().toString(),
                            "toAccountId", transaction.getToAccountId().toString(),
                            "amount", transaction.getAmount()
                    ))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException("Account Service error: " + errorBody))))
                    .bodyToMono(Void.class)
                    .block();

            transaction.setStatus(TransactionStatus.SUCCESS);
        } catch (RuntimeException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            System.err.println("Transaction execution failed for ID " + transactionId + ": " + e.getMessage());
            throw new RuntimeException("Transaction execution failed: " + e.getMessage(), e);
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            System.err.println("An unexpected error occurred during transaction execution for ID " + transactionId + ": " + e.getMessage());
            throw new RuntimeException("An unexpected error occurred during transaction execution: " + e.getMessage(), e);
        } finally {
            // Persist the status update (SUCCESS or FAILED)
            transactionRepository.save(transaction);
            logToKafka("Response", transaction);
        }
        return transaction;
    }

    @Override
    public List<Transaction> getTransactionsForAccount(UUID accountId) {
        List<Transaction> transactions = transactionRepository.findByFromAccountIdOrToAccountIdOrderByTimestampDesc(accountId, accountId);
        return transactions;
    }

    private void logToKafka(String messageType, Object payload) {
        try {
            String jsonPayload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            String logMessage = String.format("{\"message\": \"%s\", \"messageType\": \"%s\", \"dateTime\": \"%s\"}",
                    escapeJson(jsonPayload), messageType, Instant.now().toString());
            kafkaTemplate.send(loggingKafkaTopic, logMessage);
        } catch (Exception e) {
            System.err.println("Failed to send log to Kafka: " + e.getMessage());
        }
    }

    private String escapeJson(String json) {
        return json.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
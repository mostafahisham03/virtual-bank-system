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
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

        @Value("${account.service.url:http://localhost:8082}")
        private String accountServiceUrl;

        // -------------------------
        // Initiate Transfer
        // -------------------------
        @Override
        @Transactional
        public InitiateTransferResponse initiateTransfer(
                        UUID fromAccountId, UUID toAccountId, BigDecimal amount, String description) {

                kafkaLogger.sendLog(
                                "initiateTransfer request: from=" + fromAccountId + ", to=" + toAccountId + ", amount="
                                                + amount,
                                "Request");

                // Basic validations (null check can be done in controller via @Valid; we only
                // check <= 0 here)
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BadRequestException("Amount must be greater than zero.");
                }
                if (fromAccountId.equals(toAccountId)) {
                        throw new BadRequestException("Cannot transfer to the same account.");
                }

                WebClient webClient = webClientBuilder.baseUrl(accountServiceUrl).build();

                // Validate source account & get its details (need balance)
                AccountDetails fromAccount = fetchAccount(webClient, fromAccountId);

                // Validate destination account exists (no need to keep the object—avoid “unused
                // variable” warning)
                ensureAccountExists(webClient, toAccountId);

                // Balance check
                if (fromAccount.getBalance().compareTo(amount) < 0) {
                        throw new BadRequestException("Insufficient funds.");
                }

                // Create INITIATED transaction
                Transaction transaction = Transaction.builder()
                                .fromAccountId(fromAccountId)
                                .toAccountId(toAccountId)
                                .amount(amount)
                                .description(description)
                                .status(TransactionStatus.INITIATED)
                                .timestamp(Instant.now())
                                .build();

                Transaction saved = transactionRepository.save(transaction);

                kafkaLogger.sendLog(
                                "initiateTransfer response: id=" + saved.getId() + ", status=" + saved.getStatus(),
                                "Response");

                return new InitiateTransferResponse(saved.getId(), saved.getStatus(), saved.getTimestamp());
        }

        // -------------------------
        // Execute Transfer
        // -------------------------
        @Override
        @Transactional
        public ExecuteTransferResponse executeTransfer(UUID transactionId) {
                kafkaLogger.sendLog("executeTransfer request: txId=" + transactionId, "Request");

                Transaction transaction = transactionRepository
                                .findByIdAndStatus(transactionId, TransactionStatus.INITIATED)
                                .orElseThrow(ResourceNotFoundException::new);
                WebClient webClient = webClientBuilder.baseUrl(accountServiceUrl).build();

                AccountBalanceUpdateDTO updateDto = new AccountBalanceUpdateDTO(
                                transaction.getFromAccountId(),
                                transaction.getToAccountId(),
                                transaction.getAmount());

                kafkaLogger.sendLog("accounts.transfer request: " + updateDto.toString(), "Request");

                webClient.put()
                                .uri("/accounts/transfer")
                                .bodyValue(updateDto)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse
                                                .bodyToMono(String.class).flatMap(body -> {
                                                        // Mark FAILED in a new tx, then signal error
                                                        markTransactionAsFailed(transaction,
                                                                        "Account service error: "
                                                                                        + clientResponse.statusCode()
                                                                                                        .value());
                                                        return Mono.error(new BadRequestException(
                                                                        "Failed to transfer funds: "
                                                                                        + body));
                                                }))
                                .bodyToMono(Void.class)
                                .block();

                // Success
                transaction.setStatus(TransactionStatus.SUCCESS);
                transaction.setTimestamp(Instant.now());
                transactionRepository.save(transaction);

                kafkaLogger.sendLog(
                                "executeTransfer response: txId=" + transactionId + ", status="
                                                + transaction.getStatus(),
                                "Response");

                return new ExecuteTransferResponse(transaction.getId(), transaction.getStatus(),
                                transaction.getTimestamp());
        }

        // -------------------------
        // Get Transactions for Account
        // -------------------------
        @Override
        public List<TransactionResponse> getTransactionsForAccount(UUID accountId) {
                kafkaLogger.sendLog("getTransactionsForAccount request: accountId=" + accountId, "Request");

                List<Transaction> transactions = transactionRepository
                                .findByFromAccountIdOrToAccountIdOrderByTimestampDesc(accountId, accountId);

                if (transactions.isEmpty()) {
                        throw new ResourceNotFoundException("No transactions found for account ID: " + accountId);
                }

                List<TransactionResponse> response = transactions.stream()
                                .map(t -> new TransactionResponse(
                                                t.getId(),
                                                t.getFromAccountId(),
                                                t.getToAccountId(),
                                                t.getAmount(),
                                                t.getStatus(),
                                                t.getTimestamp(),
                                                t.getDescription()))
                                .toList();

                kafkaLogger.sendLog(
                                "getTransactionsForAccount response: count=" + response.size() + " for accountId="
                                                + accountId,
                                "Response");

                return response;
        }

        // -------------------------
        // Helpers
        // -------------------------

        /**
         * Fetch full account details; 404 -> ResourceNotFound, other non-2xx ->
         * BadRequest.
         */
        private AccountDetails fetchAccount(WebClient webClient, UUID accountId) {
                return webClient.get()
                                .uri("/accounts/{accountId}", accountId)
                                .retrieve()
                                .onStatus(code -> code.value() == 404,
                                                resp -> Mono.error(new ResourceNotFoundException("Account not found.")))
                                .onStatus(HttpStatusCode::isError,
                                                resp -> Mono.error(new BadRequestException("Failed to fetch account.")))
                                .bodyToMono(AccountDetails.class)
                                .block();
        }

        /**
         * Only ensure the account exists; discard body to avoid “unused variable”
         * warnings.
         */
        private void ensureAccountExists(WebClient webClient, UUID accountId) {
                webClient.get()
                                .uri("/accounts/{accountId}", accountId)
                                .retrieve()
                                .onStatus(code -> code.value() == 404,
                                                resp -> Mono.error(new ResourceNotFoundException("Account not found.")))
                                .onStatus(HttpStatusCode::isError,
                                                resp -> Mono.error(new BadRequestException("Failed to fetch account.")))
                                .toBodilessEntity()
                                .block();
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void markTransactionAsFailed(Transaction transaction, String reason) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setTimestamp(Instant.now());
                transactionRepository.save(transaction);
                kafkaLogger.sendLog("markTransactionAsFailed: txId=" + transaction.getId() + ", reason=" + reason,
                                "Info");
        }
}

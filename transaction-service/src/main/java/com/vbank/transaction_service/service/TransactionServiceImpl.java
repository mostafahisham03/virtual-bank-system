package com.vbank.transaction_service.service; // Defines the package for this class

import com.fasterxml.jackson.databind.ObjectMapper; // Imports ObjectMapper for converting Java objects to JSON strings
import com.vbank.transaction_service.model.Transaction; // Imports the Transaction entity (data model)
import com.vbank.transaction_service.model.TransactionStatus; // Imports the enum for transaction statuses
import com.vbank.transaction_service.repository.TransactionRepository; // Imports the repository for database operations
import com.vbank.transaction_service.config.KafkaLogger;
import com.vbank.transaction_service.dto.AccountBalanceUpdateDTO; // Imports the DTO for updating account balances in Account Service
import lombok.RequiredArgsConstructor; // Lombok annotation to automatically generate a constructor for final fields
import org.springframework.beans.factory.annotation.Value; // Spring annotation to inject values from properties files
import org.springframework.kafka.core.KafkaTemplate; // Spring Kafka component for sending messages to Kafka
import org.springframework.stereotype.Service; // Spring annotation to mark this class as a service component
import org.springframework.transaction.annotation.Transactional; // Spring annotation to manage database transactions
import org.springframework.web.reactive.function.client.WebClient; // Spring's reactive HTTP client for inter-service communication
import org.springframework.web.reactive.function.client.WebClientResponseException; // Specific exception for HTTP client errors
import reactor.core.publisher.Mono; // Reactor type for representing a stream of 0 or 1 element, used with WebClient
import org.springframework.http.HttpStatus; // ADD THIS IMPORT for HttpStatus enum

import java.math.BigDecimal; // For handling precise decimal numbers (currency amounts)
import java.time.Instant; // For representing a point in time (timestamps)
import java.util.List; // For lists of objects (e.g., transactions history)
import java.util.UUID; // For generating universally unique identifiers

@Service // This annotation tells Spring that this class is a service component,
         // responsible for business logic.
@RequiredArgsConstructor // Lombok generates a constructor that injects all 'final' fields (dependencies)
                         // automatically.
public class TransactionServiceImpl implements TransactionService { // Implements the TransactionService interface,
                                                                    // defining its contract.

        private final TransactionRepository transactionRepository; // Dependency: Used to interact with the database for
                                                                   // Transaction entities.
        private final WebClient.Builder webClientBuilder; // Dependency: Used to build instances of WebClient for making
                                                          // HTTP calls to other services.
        private final KafkaTemplate<String, String> kafkaTemplate; // Dependency: Used to send messages (logs) to Kafka
                                                                   // topics.
        private final ObjectMapper objectMapper; // Dependency: Used to convert Java objects to JSON strings (for
                                                 // logging).
        private final KafkaLogger kafkaLogger; // Dependency: Custom logger for sending structured logs to Kafka.
        // Injects the Kafka topic name for logging from application.properties, with a
        // default value.
        @Value("${logging.kafka.topic:vbank-logs}")
        private String loggingKafkaTopic;

        // Injects the URL of the Account Service from application.properties, with a
        // default value.
        @Value("${account.service.url:http://localhost:8082}")
        private String accountServiceUrl;

        @Override // Indicates that this method implements a method from the TransactionService
                  // interface.
        @Transactional // This annotation ensures that the method runs within a database transaction.
                       // If any part of the method fails (throws an exception), all changes made to
                       // the database within this method will be rolled back.
        public Transaction initiateTransfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount,
                        String description) {
                // Validates that the transfer amount is positive. Throws an
                // IllegalArgumentException if not.
                kafkaLogger.sendLog("Initiating transfer from " + fromAccountId + " to " + toAccountId
                                + " for amount: " + amount + " with description: " + description, "Request");
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Transfer amount must be positive.");
                }

                // Uses Lombok's @Builder to create a new Transaction object. This is a clean
                // way to instantiate objects.
                Transaction transaction = Transaction.builder()
                                .fromAccountId(fromAccountId) // Sets the source account ID.
                                .toAccountId(toAccountId) // Sets the destination account ID.
                                .amount(amount) // Sets the transfer amount.
                                .description(description) // Sets the transaction description.
                                .status(TransactionStatus.INITIATED) // Sets the initial status of the transaction to
                                                                     // INITIATED.
                                .timestamp(Instant.now()) // Sets the timestamp to the current UTC time.
                                .build(); // Builds the Transaction object.

                // Saves the newly created transaction to the database. This persists the
                // 'INITIATED' state.
                Transaction savedTransaction = transactionRepository.save(transaction);
                // Logs the initiation request to Kafka, sending the entire saved transaction
                // object as payload.
                kafkaLogger.sendLog("Transaction initiated: " + savedTransaction.getId(), "Response");
                return savedTransaction; // Returns the saved transaction object.
        }

        @Override // Indicates that this method implements a method from the TransactionService
                  // interface.
        @Transactional // Ensures the execution logic (DB update + external call) runs atomically.
        public Transaction executeTransfer(UUID transactionId) {
                // Retrieves the transaction from the database by its ID, ensuring it's in the
                // 'INITIATED' status.
                // If not found or status is not 'INITIATED', it throws an
                // IllegalArgumentException.
                kafkaLogger.sendLog("Executing transfer for transaction ID: " + transactionId, "Request");
                Transaction transaction = transactionRepository
                                .findByIdAndStatus(transactionId, TransactionStatus.INITIATED)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Transaction not found or not in 'INITIATED' status with ID: "
                                                                + transactionId));

                try {
                        // Builds a WebClient instance with the base URL of the Account Service.
                        WebClient webClient = webClientBuilder.baseUrl(accountServiceUrl).build();

                        // Creates a Data Transfer Object (DTO) containing the necessary information for
                        // the Account Service.
                        AccountBalanceUpdateDTO updateDto = new AccountBalanceUpdateDTO(
                                        transaction.getFromAccountId(),
                                        transaction.getToAccountId(),
                                        transaction.getAmount());

                        // Logs the DTO being sent to the Account Service for auditing.
                        kafkaLogger.sendLog(
                                        "Sending balance update request to Account Service: " + updateDto.toString(),
                                        "Request");

                        // Performs an HTTP PUT request to the Account Service to update balances.
                        webClient.put()
                                        .uri("/accounts/transfer") // The specific endpoint on the Account Service.
                                        .bodyValue(updateDto) // Sends the AccountBalanceUpdateDTO as the request body.
                                        .retrieve() // Initiates the retrieval of the response.
                                        // Handles HTTP 4xx (client error) or 5xx (server error) responses from the
                                        // Account Service.
                                        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                                        clientResponse -> clientResponse.bodyToMono(String.class) // Extracts
                                                                                                                  // the
                                                                                                                  // error
                                                                                                                  // body
                                                                                                                  // as
                                                                                                                  // a
                                                                                                                  // String.
                                                                        .flatMap(errorBody -> { // Processes the error
                                                                                                // body.
                                                                                System.err.println(
                                                                                                "Account Service returned an error ("
                                                                                                                + clientResponse.statusCode()
                                                                                                                + "): "
                                                                                                                + errorBody);

                                                                                // FIX STARTS HERE
                                                                                // Get the integer status code
                                                                                int statusCodeValue = clientResponse
                                                                                                .statusCode().value();
                                                                                // Resolve to HttpStatus enum (if
                                                                                // possible) to get the reason phrase
                                                                                HttpStatus httpStatus = HttpStatus
                                                                                                .resolve(statusCodeValue);
                                                                                // Use the reason phrase if resolved,
                                                                                // otherwise a default
                                                                                String reasonPhrase = (httpStatus != null)
                                                                                                ? httpStatus.getReasonPhrase()
                                                                                                : "Unknown Error";
                                                                                // FIX ENDS HERE

                                                                                // Throws a WebClientResponseException,
                                                                                // which is a more specific and useful
                                                                                // exception for HTTP errors.
                                                                                return Mono.error(
                                                                                                new WebClientResponseException(
                                                                                                                "Account Service call failed: "
                                                                                                                                + errorBody, // Detailed
                                                                                                                                             // error
                                                                                                                                             // message.
                                                                                                                statusCodeValue, // HTTP
                                                                                                                                 // status
                                                                                                                                 // code
                                                                                                                                 // (e.g.,
                                                                                                                                 // 400).
                                                                                                                reasonPhrase, // Use
                                                                                                                              // the
                                                                                                                              // resolved
                                                                                                                              // reason
                                                                                                                              // phrase
                                                                                                                              // here
                                                                                                                null,
                                                                                                                null,
                                                                                                                null // Other
                                                                                                                     // headers/body
                                                                                                                     // details
                                                                                                                     // can
                                                                                                                     // be
                                                                                                                     // populated
                                                                                                                     // here
                                                                                                                     // if
                                                                                                                     // needed.
                                                                                ));
                                                                        }))
                                        .bodyToMono(Void.class) // Expects no response body on a successful call (e.g.,
                                                                // HTTP 200 OK).
                                        .block(); // Blocks the execution until the HTTP call completes. In a fully
                                                  // reactive
                                                  // system, you'd chain reactive operations here instead of blocking.
                        // If the WebClient call succeeds, updates the transaction status to SUCCESS.
                        transaction.setStatus(TransactionStatus.SUCCESS);
                        transaction.setTimestamp(Instant.now()); // Updates the timestamp to indicate completion time.
                        transactionRepository.save(transaction); // Saves the updated transaction status to the
                                                                 // database.
                        kafkaLogger.sendLog("Transfer executed successfully for transaction ID: " + transactionId,
                                        "Response");
                        return transaction; // Returns the successfully executed transaction.

                } catch (WebClientResponseException e) {
                        // Catches exceptions specifically from WebClient HTTP errors (e.g., 400 from
                        // Account Service).
                        transaction.setStatus(TransactionStatus.FAILED); // Sets the transaction status to FAILED.
                        transaction.setTimestamp(Instant.now()); // Updates timestamp.
                        transactionRepository.save(transaction); // Saves the FAILED status to the database.
                        // Logs the failure with the error body from the Account Service.
                        // logToKafka("Response_Execution_Failed_WebClient",
                        // e.getResponseBodyAsString() != null ? e.getResponseBodyAsString()
                        // : e.getMessage());
                        System.err.println("Transaction execution failed for ID " + transactionId
                                        + " due to Account Service error: " + e.getMessage());
                        // Re-throws an IllegalArgumentException, passing the root cause for better
                        // error propagation.
                        throw new IllegalArgumentException("Transfer failed: " + e.getMessage(), e);
                } catch (Exception e) {
                        // Catches any other unexpected exceptions that might occur during the process.
                        transaction.setStatus(TransactionStatus.FAILED); // Sets the transaction status to FAILED.
                        transaction.setTimestamp(Instant.now()); // Updates timestamp.
                        transactionRepository.save(transaction); // Saves the FAILED status to the database.
                        // logToKafka("Response_Execution_Failed_Unexpected", e.getMessage()); // Logs
                        // the unexpected
                        // failure.
                        System.err.println("An unexpected error occurred during transaction execution for ID "
                                        + transactionId
                                        + ": " + e.getMessage());
                        // Throws a generic RuntimeException.
                        throw new RuntimeException(
                                        "An unexpected error occurred during transaction execution: " + e.getMessage(),
                                        e);
                }
                // No 'finally' block for saving status is needed here because save(transaction)
                // is handled in both success and catch blocks.
        }

        @Override // Indicates that this method implements a method from the TransactionService
                  // interface.
        public List<Transaction> getTransactionsForAccount(UUID accountId) {
                // Retrieves a list of transactions where the given account is either the 'from'
                // or 'to' account,
                // ordered by timestamp in descending order (most recent first).
                kafkaLogger.sendLog("Fetching transactions for account ID: " + accountId, "Request");
                List<Transaction> transactions = transactionRepository
                                .findByFromAccountIdOrToAccountIdOrderByTimestampDesc(accountId, accountId);
                kafkaLogger.sendLog("Fetched " + transactions.size() + " transactions for account ID: " + accountId,
                                "Response");
                return transactions; // Returns the list of transactions.
        }

        /**
         * Helper method to send structured log messages to Kafka.
         * The `payload` object will be serialized to JSON and included as the 'message'
         * field in the Kafka log.
         * This provides richer log data than just a simple string, making it easier for
         * log aggregation systems to parse.
         * 
         * @param messageType A string indicating the type of log message (e.g.,
         *                    "Request_Initiation", "Response_Execution_Success").
         * @param payload     The Java object to be serialized into the 'message' field
         *                    of the Kafka log.
         */
        private void logToKafka(String messageType, Object payload) {
                try {
                        // Serializes the Java payload object into a JSON string.
                        String jsonPayload = objectMapper.writeValueAsString(payload);
                        // Constructs the final log message string in JSON format, embedding the
                        // serialized payload.
                        String logMessage = String.format(
                                        "{\"message\": \"%s\", \"messageType\": \"%s\", \"dateTime\": \"%s\"}",
                                        escapeJson(jsonPayload), messageType, Instant.now().toString());
                        // Sends the constructed log message to the configured Kafka topic.
                        kafkaTemplate.send(loggingKafkaTopic, logMessage);
                } catch (Exception e) {
                        // Prints an error message to the console if sending the log to Kafka fails.
                        // This is important so the main application logic isn't interrupted by logging
                        // failures.
                        System.err.println("Failed to send log to Kafka: " + e.getMessage());
                }
        }

        /**
         * Escapes special characters within a given JSON string.
         * This is crucial when embedding one JSON string (e.g., `jsonPayload`) inside
         * another JSON string (the `logMessage`).
         * Without proper escaping, the outer JSON would break.
         * 
         * @param json The JSON string to escape.
         * @return The escaped JSON string, safe for embedding.
         */
        private String escapeJson(String json) {
                return json.replace("\\", "\\\\") // Replaces single backslashes with double backslashes.
                                .replace("\"", "\\\"") // Escapes double quotes.
                                .replace("\n", "\\n") // Escapes newline characters.
                                .replace("\r", "\\r") // Escapes carriage return characters.
                                .replace("\t", "\\t"); // Escapes tab characters.
        }
}
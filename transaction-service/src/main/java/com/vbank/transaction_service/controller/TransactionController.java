package com.vbank.transaction_service.controller;

import com.vbank.transaction_service.model.Transaction; // Corrected import
import com.vbank.transaction_service.service.TransactionService; // Corrected import
import com.vbank.transaction_service.dto.InitiateTransferRequest; // Import InitiateTransferRequest DTO
import com.vbank.transaction_service.dto.ExecuteTransferRequest; // Import ExecuteTransferRequest DTO
import com.vbank.transaction_service.dto.ErrorResponse; // Import ErrorResponse DTO
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid; // Import for validation

import java.util.List;
import java.util.UUID;

@RestController // Marks this class as a REST controller, handling incoming HTTP requests
@RequestMapping("/transactions") // Base path for all endpoints in this controller
@RequiredArgsConstructor // Lombok: Generates a constructor with final fields, replacing explicit constructor
public class TransactionController {

    private final TransactionService transactionService; // Dependency on the TransactionService

    @PostMapping("/transfer/initiation") // Maps HTTP POST requests to /transactions/transfer/initiation
    public ResponseEntity<?> initiateTransfer(@Valid @RequestBody InitiateTransferRequest request) { // Use DTO and @Valid
        try {
            // No manual parsing or validation needed here due to @Valid and DTO
            Transaction initiatedTransaction = transactionService.initiateTransfer(
                    request.getFromAccountId(),
                    request.getToAccountId(),
                    request.getAmount(),
                    request.getDescription()
            );

            // Return 200 OK with the initiated transaction details.
            // Spring will convert this Transaction object to JSON.
            return ResponseEntity.ok(initiatedTransaction);
        } catch (IllegalArgumentException e) {
            // Handle business logic errors (e.g., amount not positive, caught by service)
            return ResponseEntity.badRequest().body(createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            // Handle unexpected internal errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/transfer/execution") // Maps HTTP POST requests to /transactions/transfer/execution
    public ResponseEntity<?> executeTransfer(@Valid @RequestBody ExecuteTransferRequest request) { // Use DTO and @Valid
        try {
            // No manual parsing or validation needed here due to @Valid and DTO
            Transaction executedTransaction = transactionService.executeTransfer(request.getTransactionId());

            // Return 200 OK with the executed transaction details.
            // Spring will convert this Transaction object to JSON.
            return ResponseEntity.ok(executedTransaction);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Handle business logic errors (e.g., transaction not found, invalid status)
            return ResponseEntity.badRequest().body(createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage()));
        } catch (RuntimeException e) {
            // Catch RuntimeException from service (e.g., WebClient failure, re-thrown as RuntimeException)
            // It's often better to map specific service exceptions to specific HTTP statuses.
            // For now, mapping to BAD_REQUEST as per your original code's intent for RuntimeException.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            // Handle any other unexpected internal errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during transaction execution: " + e.getMessage()));
        }
    }

    @GetMapping("/accounts/{accountId}/transactions") // Maps HTTP GET requests to /transactions/accounts/{accountId}/transactions
    public ResponseEntity<?> getTransactionsForAccount(@PathVariable UUID accountId) {
        try {
            List<Transaction> transactions = transactionService.getTransactionsForAccount(accountId);
            if (transactions.isEmpty()) {
                // Return 404 Not Found if no transactions are found
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(HttpStatus.NOT_FOUND, "No transactions found for account ID " + accountId + "."));
            }
            return ResponseEntity.ok(transactions); // Return 200 OK with the list of transactions
        } catch (Exception e) {
            // Handle unexpected internal errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage()));
        }
    }

    /**
     * Helper method to create a standardized ErrorResponse DTO.
     * This ensures consistent error message format across the API.
     * @param status The HTTP status code.
     * @param message A human-readable error message.
     * @return An ErrorResponse object.
     */
    private ErrorResponse createErrorResponse(HttpStatus status, String message) {
        // Uses the ErrorResponse DTO that we defined previously
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message);
    }
}
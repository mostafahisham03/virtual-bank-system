package com.vbank.transaction_service.controller; // Corrected package name

import com.vbank.transaction_service.model.Transaction; // Corrected import
import com.vbank.transaction_service.service.TransactionService; // Corrected import
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer/initiation")
    public ResponseEntity<?> initiateTransfer(@RequestBody Map<String, Object> requestBody) {
        try {
            if (!requestBody.containsKey("fromAccountId") || ! (requestBody.get("fromAccountId") instanceof String) ||
                    !requestBody.containsKey("toAccountId") || ! (requestBody.get("toAccountId") instanceof String) ||
                    !requestBody.containsKey("amount") || !(requestBody.get("amount") instanceof Number || requestBody.get("amount") instanceof String)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Bad Request",
                        "message", "Missing or invalid required fields: fromAccountId, toAccountId, amount."
                ));
            }

            UUID fromAccountId = UUID.fromString((String) requestBody.get("fromAccountId"));
            UUID toAccountId = UUID.fromString((String) requestBody.get("toAccountId"));
            BigDecimal amount = new BigDecimal(requestBody.get("amount").toString());
            String description = (String) requestBody.get("description");

            Transaction transaction = transactionService.initiateTransfer(fromAccountId, toAccountId, amount, description);
            return ResponseEntity.ok(Map.of(
                    "transactionId", transaction.getTransactionId().toString(),
                    "status", transaction.getStatus().toString(),
                    "timestamp", transaction.getTimestamp().toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", "Bad Request",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "error", "Internal Server Error",
                    "message", "An unexpected error occurred: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/transfer/execution")
    public ResponseEntity<?> executeTransfer(@RequestBody Map<String, Object> requestBody) {
        try {
            if (!requestBody.containsKey("transactionId") || !(requestBody.get("transactionId") instanceof String)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Bad Request",
                        "message", "Missing or invalid required field: transactionId."
                ));
            }

            UUID transactionId = UUID.fromString((String) requestBody.get("transactionId"));
            Transaction transaction = transactionService.executeTransfer(transactionId);
            return ResponseEntity.ok(Map.of(
                    "transactionId", transaction.getTransactionId().toString(),
                    "status", transaction.getStatus().toString(),
                    "timestamp", transaction.getTimestamp().toString()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", "Bad Request",
                    "message", e.getMessage()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", "Bad Request",
                    "message", e.getMessage()
            ));
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "error", "Internal Server Error",
                    "message", "An unexpected error occurred during transaction execution: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<?> getTransactionsForAccount(@PathVariable UUID accountId) {
        try {
            List<Transaction> transactions = transactionService.getTransactionsForAccount(accountId);
            if (transactions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", HttpStatus.NOT_FOUND.value(),
                        "error", "Not Found",
                        "message", "No transactions found for account ID " + accountId + "."
                ));
            }
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "error", "Internal Server Error",
                    "message", "An unexpected error occurred: " + e.getMessage()
            ));
        }
    }
}
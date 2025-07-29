package com.vbank.transaction_service.controller;

import com.vbank.transaction_service.service.TransactionService; // Corrected import
import com.vbank.transaction_service.dto.InitiateTransferRequest; // Import InitiateTransferRequest DTO
import com.vbank.transaction_service.dto.InitiateTransferResponse;
import com.vbank.transaction_service.dto.TransactionResponse;
import com.vbank.transaction_service.dto.ExecuteTransferRequest; // Import ExecuteTransferRequest DTO
import com.vbank.transaction_service.dto.ExecuteTransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid; // Import for validation

import java.util.List;
import java.util.UUID;

@RestController // Marks this class as a REST controller, handling incoming HTTP requests
@RequestMapping("/transactions") // Base path for all endpoints in this controller
@RequiredArgsConstructor // Lombok: Generates a constructor with final fields, replacing explicit
                         // constructor
public class TransactionController {

    private final TransactionService transactionService; // Dependency on the TransactionService

    @PostMapping("/transfer/initiation") // Maps HTTP POST requests to /transactions/transfer/initiation
    public ResponseEntity<InitiateTransferResponse> initiateTransfer(
            @Valid @RequestBody InitiateTransferRequest request) {
        InitiateTransferResponse response = transactionService.initiateTransfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getDescription());
        return ResponseEntity.status(201).body(response);

    }

    @PostMapping("/transfer/execution") // Maps HTTP POST requests to /transactions/transfer/execution
    public ResponseEntity<ExecuteTransferResponse> executeTransfer(@Valid @RequestBody ExecuteTransferRequest request) {
        ExecuteTransferResponse response = transactionService.executeTransfer(request.getTransactionId());
        return ResponseEntity.ok(response); // Return 200 OK with the response
    }

    @GetMapping("/accounts/{accountId}") // Maps HTTP GET requests to /transactions/accounts/{accountId}/transactions
    public ResponseEntity<List<TransactionResponse>> getTransactionsForAccount(@PathVariable UUID accountId) {
        List<TransactionResponse> transactions = transactionService.getTransactionsForAccount(accountId);
        return ResponseEntity.ok(transactions); // Return 200 OK with the list of transactions
    }
}
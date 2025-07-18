package com.vbank.account_service.controller;

import com.vbank.account_service.dto.*;
import com.vbank.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@RequestBody AccountRequest request) {
        AccountResponse account = accountService.createAccount(request);
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> get(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<AccountResponse>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @PutMapping("/transfer")
    public ResponseEntity<Map<String, String>> transfer(@RequestBody TransferRequest request) {
        accountService.transferFunds(request);
        return ResponseEntity.ok(Map.of("message", "Transfer completed successfully."));
    }
}

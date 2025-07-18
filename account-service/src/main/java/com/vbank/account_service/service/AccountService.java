package com.vbank.account_service.service;

import com.vbank.account_service.dto.*;

import java.util.List;
import java.util.UUID;

public interface AccountService {
    AccountResponse createAccount(AccountRequest request);

    AccountResponse getAccountById(UUID accountId);

    List<AccountResponse> getAccountsByUserId(UUID userId);

    void transferFunds(TransferRequest request);

}

package com.eventledger.account.interfaces.ledger.service;

import com.eventledger.account.interfaces.ledger.dto.AccountResponse;
import com.eventledger.account.interfaces.ledger.dto.BalanceResponse;

public interface LedgerService {

    BalanceResponse getBalance(String accountId);

    AccountResponse getAccountDetails(String accountId);
}
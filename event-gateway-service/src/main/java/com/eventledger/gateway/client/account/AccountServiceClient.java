package com.eventledger.gateway.client.account;

import com.eventledger.gateway.client.dto.AccountBalanceResponse;
import com.eventledger.gateway.client.dto.AccountTransactionRequest;
import com.eventledger.gateway.client.dto.AccountTransactionResponse;

public interface AccountServiceClient {

    AccountTransactionResponse applyTransaction(String accountId, AccountTransactionRequest request);

    AccountBalanceResponse getBalance(String accountId);
}
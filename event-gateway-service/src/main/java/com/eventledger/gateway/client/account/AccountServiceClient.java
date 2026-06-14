package com.eventledger.gateway.client.account;

import com.eventledger.gateway.client.dto.AccountBalanceResponse;
import com.eventledger.gateway.client.dto.AccountTransactionRequest;
import com.eventledger.gateway.client.dto.AccountTransactionResponse;

/**
 * Description: AccountServiceClient.java defines the contract for all outbound communication
 * from the Event Gateway Service to the Account Service. It abstracts transaction application
 * and balance retrieval operations, allowing implementations to apply resiliency patterns
 * such as circuit breaking without exposing those details to callers.
 */
public interface AccountServiceClient {

    /**
     * Sends a transaction to the Account Service to be applied against the specified account.
     * The call is protected by a circuit breaker — throws AccountServiceUnavailableException
     * if the Account Service is unavailable or the circuit is open.
     *
     * @param accountId the target account ID
     * @param request   the transaction details to apply
     * @return AccountTransactionResponse with the result from the Account Service
     */
    AccountTransactionResponse applyTransaction(String accountId, AccountTransactionRequest request);

    /**
     * Retrieves the current balance for the specified account from the Account Service.
     * The call is protected by a circuit breaker — throws AccountServiceUnavailableException
     * if the Account Service is unavailable or the circuit is open.
     *
     * @param accountId the account whose balance is requested
     * @return AccountBalanceResponse containing the accountId and computed balance
     */
    AccountBalanceResponse getBalance(String accountId);
}
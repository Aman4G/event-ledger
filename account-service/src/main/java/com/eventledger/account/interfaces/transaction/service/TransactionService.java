package com.eventledger.account.interfaces.transaction.service;

import com.eventledger.account.interfaces.transaction.dto.TransactionRequest;
import com.eventledger.account.interfaces.transaction.dto.TransactionResponse;

import java.math.BigDecimal;

/**
 * Description: TransactionService.java defines the contract for transaction processing
 * operations in the Account Service. It handles applying transactions to accounts with
 * idempotency enforcement using eventId as the primary key, and computing account balances
 * as the net difference between total credits and total debits across all stored transactions.
 */
public interface TransactionService {

    /**
     * Applies a transaction to the specified account. If a transaction with the same eventId
     * already exists, returns it as a duplicate without modifying the account balance.
     * Auto-creates the account record if it does not yet exist.
     *
     * @param accountId the target account ID from the request path
     * @param request   the transaction payload containing eventId, type, amount, currency,
     *                  and eventTimestamp
     * @return TransactionResponse with status APPLIED for new transactions or
     *         DUPLICATE_IGNORED for duplicates
     */
    TransactionResponse applyTransaction(String accountId, TransactionRequest request);

    /**
     * Computes the current balance for the given account as:
     * sum(CREDIT amounts) − sum(DEBIT amounts).
     * Results are always consistent regardless of transaction insertion order.
     *
     * @param accountId the account whose balance should be calculated
     * @return the net balance as a BigDecimal
     */
    BigDecimal calculateBalance(String accountId);
}
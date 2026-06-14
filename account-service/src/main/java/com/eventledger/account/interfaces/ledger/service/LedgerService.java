package com.eventledger.account.interfaces.ledger.service;

import com.eventledger.account.interfaces.ledger.dto.AccountResponse;
import com.eventledger.account.interfaces.ledger.dto.BalanceResponse;

/**
 * Description: LedgerService.java defines the contract for account ledger read operations
 * in the Account Service. It provides balance retrieval and full account detail queries,
 * validating account existence before delegating to the underlying repositories and
 * transaction service.
 */
public interface LedgerService {

    /**
     * Returns the current net balance for the given account as sum(CREDITs) − sum(DEBITs).
     *
     * @param accountId the account whose balance is requested
     * @return BalanceResponse containing the accountId and computed balance
     * @throws com.eventledger.account.common.exception.ResourceNotFoundException if the
     *         account does not exist
     */
    BalanceResponse getBalance(String accountId);

    /**
     * Returns full account details including all transactions sorted by eventTimestamp ascending.
     *
     * @param accountId the account whose details are requested
     * @return AccountResponse containing the accountId and the ordered list of transactions
     * @throws com.eventledger.account.common.exception.ResourceNotFoundException if the
     *         account does not exist
     */
    AccountResponse getAccountDetails(String accountId);
}
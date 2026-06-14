package com.eventledger.account.interfaces.ledger.service.impl;

import com.eventledger.account.common.exception.ResourceNotFoundException;
import com.eventledger.account.interfaces.ledger.dto.AccountResponse;
import com.eventledger.account.interfaces.ledger.dto.BalanceResponse;
import com.eventledger.account.interfaces.ledger.repository.AccountRepository;
import com.eventledger.account.interfaces.ledger.service.LedgerService;
import com.eventledger.account.interfaces.transaction.entity.Transaction;
import com.eventledger.account.interfaces.transaction.repository.TransactionRepository;
import com.eventledger.account.interfaces.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Description: LedgerServiceImpl.java is the service implementation for account ledger read
 * operations in the Account Service. It validates account existence before any query,
 * delegates balance computation to TransactionService, and retrieves transaction history
 * from TransactionRepository ordered by eventTimestamp ascending to ensure chronologically
 * correct results regardless of insertion order.
 */
@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    /**
     * Returns the current net balance for the given account. Validates that the account
     * exists before delegating balance computation to TransactionService.
     *
     * @param accountId the account whose balance is requested
     * @return BalanceResponse containing the accountId and computed net balance
     * @throws com.eventledger.account.common.exception.ResourceNotFoundException if the
     *         account does not exist
     */
    @Override
    public BalanceResponse getBalance(String accountId) {
        validateAccountExists(accountId);

        return BalanceResponse.builder()
                .accountId(accountId)
                .balance(transactionService.calculateBalance(accountId))
                .build();
    }

    /**
     * Returns full account details including all transactions ordered by eventTimestamp
     * ascending. Validates that the account exists before querying transactions.
     *
     * @param accountId the account whose details are requested
     * @return AccountResponse containing the accountId and chronologically ordered transactions
     * @throws com.eventledger.account.common.exception.ResourceNotFoundException if the
     *         account does not exist
     */
    @Override
    public AccountResponse getAccountDetails(String accountId) {
        validateAccountExists(accountId);

        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByEventTimestampAsc(accountId);

        return AccountResponse.builder()
                .accountId(accountId)
                .transactions(transactions)
                .build();
    }

    /**
     * Validates that an account with the given ID exists in the repository.
     *
     * @param accountId the account ID to check
     * @throws com.eventledger.account.common.exception.ResourceNotFoundException if no
     *         account is found with the given ID
     */
    private void validateAccountExists(String accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found: " + accountId);
        }
    }
}
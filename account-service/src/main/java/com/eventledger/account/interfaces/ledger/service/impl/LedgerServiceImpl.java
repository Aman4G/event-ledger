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

@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    @Override
    public BalanceResponse getBalance(String accountId) {
        validateAccountExists(accountId);

        return BalanceResponse.builder()
                .accountId(accountId)
                .balance(transactionService.calculateBalance(accountId))
                .build();
    }

    @Override
    public AccountResponse getAccountDetails(String accountId) {
        validateAccountExists(accountId);

        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByEventTimestampAsc(accountId);

        return AccountResponse.builder()
                .accountId(accountId)
                .transactions(transactions)
                .build();
    }

    private void validateAccountExists(String accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found: " + accountId);
        }
    }
}
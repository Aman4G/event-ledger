package com.eventledger.account.interfaces.transaction.service.impl;

import com.eventledger.account.common.enums.TransactionType;
import com.eventledger.account.common.exception.AccountMismatchException;
import com.eventledger.account.interfaces.ledger.entity.Account;
import com.eventledger.account.interfaces.ledger.repository.AccountRepository;
import com.eventledger.account.interfaces.transaction.dto.TransactionRequest;
import com.eventledger.account.interfaces.transaction.dto.TransactionResponse;
import com.eventledger.account.interfaces.transaction.entity.Transaction;
import com.eventledger.account.interfaces.transaction.repository.TransactionRepository;
import com.eventledger.account.interfaces.transaction.service.TransactionService;
import com.eventledger.account.observability.TransactionMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMetricsService transactionMetricsService;

    @Override
    @Transactional
    public TransactionResponse applyTransaction(String accountId, TransactionRequest request) {
        log.info("Applying transaction eventId={} accountId={} type={} amount={}",
                request.getEventId(), accountId, request.getType(), request.getAmount());

        validateAccountId(accountId, request.getAccountId());

        return transactionRepository.findById(request.getEventId())
                .map(existingTransaction -> {
                    log.info("Duplicate transaction ignored eventId={} accountId={}",
                            request.getEventId(), accountId);
                    transactionMetricsService.incrementDuplicateTransaction();
                    return buildResponse(existingTransaction, "DUPLICATE_IGNORED");
                })
                .orElseGet(() -> createTransaction(accountId, request));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateBalance(String accountId) {
        log.info("Calculating balance for accountId={}", accountId);
        BigDecimal totalCredits = transactionRepository.sumAmountByAccountIdAndType(accountId, TransactionType.CREDIT);
        BigDecimal totalDebits = transactionRepository.sumAmountByAccountIdAndType(accountId, TransactionType.DEBIT);
        return totalCredits.subtract(totalDebits);
    }

    private TransactionResponse createTransaction(String accountId, TransactionRequest request) {
        if (!accountRepository.existsById(accountId)) {
            accountRepository.save(
                    Account.builder()
                            .accountId(accountId)
                            .build()
            );

            log.info("Account created accountId={}", accountId);
        }

        Transaction transaction = Transaction.builder()
                .eventId(request.getEventId())
                .accountId(request.getAccountId())
                .type(request.getType())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .eventTimestamp(request.getEventTimestamp())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        transactionMetricsService.incrementTransactionSuccess();

        log.info("Transaction applied successfully eventId={} accountId={}",
                savedTransaction.getEventId(), savedTransaction.getAccountId());

        return buildResponse(savedTransaction, "APPLIED");
    }

    private void validateAccountId(String pathAccountId, String requestAccountId) {
        if (!pathAccountId.equals(requestAccountId)) {
            throw new AccountMismatchException("Account ID in path does not match account ID in request body");
        }
    }

    private TransactionResponse buildResponse(Transaction transaction, String status) {
        return TransactionResponse.builder()
                .eventId(transaction.getEventId())
                .accountId(transaction.getAccountId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .eventTimestamp(transaction.getEventTimestamp())
                .status(status)
                .build();
    }
}
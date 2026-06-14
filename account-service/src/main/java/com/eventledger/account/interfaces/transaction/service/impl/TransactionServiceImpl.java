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

/**
 * Description: TransactionServiceImpl.java is the core service implementation for transaction
 * processing in the Account Service. It enforces idempotency by checking for an existing
 * transaction with the same eventId before persisting, auto-creates account records on first
 * transaction, validates that the path account ID matches the request body account ID, computes
 * balances via JPQL sum queries to ensure out-of-order correctness, and emits observability
 * metrics on each applied or duplicate transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMetricsService transactionMetricsService;

    /**
     * Applies a transaction to the specified account. Checks for an existing transaction
     * with the same eventId first — if found, returns it as a duplicate. Otherwise,
     * auto-creates the account if needed, persists the transaction, and increments the
     * success metric.
     *
     * @param accountId the target account ID from the request path
     * @param request   the transaction payload to apply
     * @return TransactionResponse with status APPLIED for new transactions or
     *         DUPLICATE_IGNORED for duplicates
     */
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

    /**
     * Computes the net balance for the given account as sum(CREDITs) − sum(DEBITs).
     * Uses JPQL sum queries so the result is always correct regardless of the order
     * in which transactions were inserted.
     *
     * @param accountId the account whose balance should be calculated
     * @return the net balance as a BigDecimal
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateBalance(String accountId) {
        log.info("Calculating balance for accountId={}", accountId);
        BigDecimal totalCredits = transactionRepository.sumAmountByAccountIdAndType(accountId, TransactionType.CREDIT);
        BigDecimal totalDebits = transactionRepository.sumAmountByAccountIdAndType(accountId, TransactionType.DEBIT);
        return totalCredits.subtract(totalDebits);
    }

    /**
     * Persists a new transaction to the database, creating the account record first if it
     * does not already exist. Increments the transaction success metric on completion.
     *
     * @param accountId the target account ID
     * @param request   the validated transaction payload to persist
     * @return TransactionResponse with status APPLIED
     */
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

    /**
     * Validates that the account ID in the request path matches the account ID in the
     * request body. Throws AccountMismatchException if they differ.
     *
     * @param pathAccountId    the account ID extracted from the URL path
     * @param requestAccountId the account ID present in the request body
     * @throws com.eventledger.account.common.exception.AccountMismatchException if the IDs do not match
     */
    private void validateAccountId(String pathAccountId, String requestAccountId) {
        if (!pathAccountId.equals(requestAccountId)) {
            throw new AccountMismatchException("Account ID in path does not match account ID in request body");
        }
    }

    /**
     * Builds a TransactionResponse from a stored Transaction entity and a status string.
     *
     * @param transaction the Transaction entity to convert
     * @param status      the status label to attach (e.g. APPLIED, DUPLICATE_IGNORED)
     * @return the populated TransactionResponse
     */
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
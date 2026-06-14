package com.eventledger.account.observability;

public interface TransactionMetricsService {

    void incrementTransactionSuccess();

    void incrementDuplicateTransaction();
}
package com.eventledger.account.observability.impl;

import com.eventledger.account.observability.TransactionMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

/**
 * Description: TransactionMetricsServiceImpl.java is the Micrometer-backed implementation
 * of TransactionMetricsService for the Account Service. It registers two named counters at
 * startup against the MeterRegistry — for successfully applied transactions and duplicate
 * ignored transactions — and provides increment methods used throughout the service to
 * track operational metrics exposed via the Actuator metrics endpoint.
 */
@Service
public class TransactionMetricsServiceImpl implements TransactionMetricsService {

    private final Counter transactionSuccessCounter;
    private final Counter duplicateIgnoredCounter;

    /**
     * Initializes and registers all Micrometer counters with the provided MeterRegistry.
     *
     * @param meterRegistry the Micrometer registry to register counters against
     */
    public TransactionMetricsServiceImpl(MeterRegistry meterRegistry) {

        this.transactionSuccessCounter = Counter.builder("transaction_success_count")
                .description("Number of successfully applied transactions")
                .tag("transaction_type", "success")
                .register(meterRegistry);

        this.duplicateIgnoredCounter = Counter.builder("transaction_duplicate_ignored_count")
                .description("Number of duplicate transactions ignored")
                .tag("transaction_type", "duplicate_ignored")
                .register(meterRegistry);
    }

    /**
     * Increments the transaction_success_count counter by one.
     */
    @Override
    public void incrementTransactionSuccess() {
        transactionSuccessCounter.increment();
    }

    /**
     * Increments the transaction_duplicate_ignored_count counter by one.
     */
    @Override
    public void incrementDuplicateTransaction() {
        duplicateIgnoredCounter.increment();
    }
}
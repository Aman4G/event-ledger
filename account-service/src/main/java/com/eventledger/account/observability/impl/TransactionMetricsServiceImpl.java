package com.eventledger.account.observability.impl;

import com.eventledger.account.observability.TransactionMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class TransactionMetricsServiceImpl implements TransactionMetricsService {

    private final Counter transactionSuccessCounter;
    private final Counter duplicateIgnoredCounter;

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

    @Override
    public void incrementTransactionSuccess() {
        transactionSuccessCounter.increment();
    }

    @Override
    public void incrementDuplicateTransaction() {
        duplicateIgnoredCounter.increment();
    }
}
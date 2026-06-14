package com.eventledger.account.observability;

/**
 * Description: TransactionMetricsService.java defines the contract for recording custom
 * observability metrics in the Account Service. Implementations expose Micrometer counters
 * for tracking successfully applied transactions and duplicate submissions, accessible
 * via the Spring Boot Actuator metrics endpoint.
 */
public interface TransactionMetricsService {

    /**
     * Increments the counter tracking the number of transactions successfully applied
     * to an account.
     */
    void incrementTransactionSuccess();

    /**
     * Increments the counter tracking the number of duplicate transaction submissions
     * that were ignored due to an existing eventId in the transaction repository.
     */
    void incrementDuplicateTransaction();
}
package com.eventledger.gateway.observability;

/**
 * Description: EventMetricsService.java defines the contract for recording custom observability
 * metrics in the Event Gateway Service. Implementations expose Micrometer counters for tracking
 * accepted events, duplicate submissions, and Account Service call failures, all accessible
 * via the Spring Boot Actuator metrics endpoint.
 */
public interface EventMetricsService {

    /**
     * Increments the counter tracking the number of new events successfully accepted
     * and forwarded to the Account Service.
     */
    void incrementEventAccepted();

    /**
     * Increments the counter tracking the number of duplicate event submissions that were
     * ignored due to an existing eventId in the gateway database.
     */
    void incrementDuplicateIgnored();

    /**
     * Increments the counter tracking the number of failures encountered when calling
     * the Account Service, including circuit breaker fallbacks.
     */
    void incrementAccountServiceFailure();
}
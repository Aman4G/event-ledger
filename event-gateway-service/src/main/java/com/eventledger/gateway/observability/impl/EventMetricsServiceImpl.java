package com.eventledger.gateway.observability.impl;

import com.eventledger.gateway.observability.EventMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

/**
 * Description: EventMetricsServiceImpl.java is the Micrometer-backed implementation of
 * EventMetricsService for the Event Gateway Service. It registers three named counters at
 * startup against the MeterRegistry — for accepted events, duplicate ignored events, and
 * Account Service failures — and provides increment methods used throughout the gateway
 * to track operational metrics exposed via the Actuator metrics endpoint.
 */
@Service
public class EventMetricsServiceImpl implements EventMetricsService {

    private final Counter eventAcceptedCounter;
    private final Counter duplicateIgnoredCounter;
    private final Counter accountServiceFailureCounter;

    /**
     * Initializes and registers all Micrometer counters with the provided MeterRegistry.
     *
     * @param meterRegistry the Micrometer registry to register counters against
     */
    public EventMetricsServiceImpl(MeterRegistry meterRegistry) {
        this.eventAcceptedCounter = Counter.builder("event_accepted_count")
                .description("Number of events accepted by gateway")
                .tag("event_status", "accepted")
                .register(meterRegistry);

        this.duplicateIgnoredCounter = Counter.builder("event_duplicate_ignored_count")
                .description("Number of duplicate events ignored by gateway")
                .tag("event_status", "duplicate_ignored")
                .register(meterRegistry);

        this.accountServiceFailureCounter = Counter.builder("account_service_failure_count")
                .description("Number of Account Service failures observed by gateway")
                .tag("dependency", "account-service")
                .register(meterRegistry);
    }

    /**
     * Increments the event_accepted_count counter by one.
     */
    @Override
    public void incrementEventAccepted() {
        eventAcceptedCounter.increment();
    }

    /**
     * Increments the event_duplicate_ignored_count counter by one.
     */
    @Override
    public void incrementDuplicateIgnored() {
        duplicateIgnoredCounter.increment();
    }

    /**
     * Increments the account_service_failure_count counter by one.
     */
    @Override
    public void incrementAccountServiceFailure() {
        accountServiceFailureCounter.increment();
    }
}
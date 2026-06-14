package com.eventledger.gateway.observability.impl;

import com.eventledger.gateway.observability.EventMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class EventMetricsServiceImpl implements EventMetricsService {

    private final Counter eventAcceptedCounter;
    private final Counter duplicateIgnoredCounter;
    private final Counter accountServiceFailureCounter;

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

    @Override
    public void incrementEventAccepted() {
        eventAcceptedCounter.increment();
    }

    @Override
    public void incrementDuplicateIgnored() {
        duplicateIgnoredCounter.increment();
    }

    @Override
    public void incrementAccountServiceFailure() {
        accountServiceFailureCounter.increment();
    }
}
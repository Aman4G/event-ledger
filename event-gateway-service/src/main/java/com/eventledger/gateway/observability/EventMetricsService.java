package com.eventledger.gateway.observability;

public interface EventMetricsService {

    void incrementEventAccepted();

    void incrementDuplicateIgnored();

    void incrementAccountServiceFailure();
}
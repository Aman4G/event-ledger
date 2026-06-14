package com.eventledger.gateway.client.account.impl;

import com.eventledger.gateway.client.account.AccountServiceClient;
import com.eventledger.gateway.client.dto.AccountBalanceResponse;
import com.eventledger.gateway.client.dto.AccountTransactionRequest;
import com.eventledger.gateway.client.dto.AccountTransactionResponse;
import com.eventledger.gateway.common.constants.TracingConstants;
import com.eventledger.gateway.common.exception.AccountServiceUnavailableException;
import com.eventledger.gateway.common.utils.ServiceBroker;
import com.eventledger.gateway.observability.EventMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountServiceClientImpl implements AccountServiceClient {

    private final ServiceBroker serviceBroker;

    private final EventMetricsService eventMetricsService;

    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${account.service.base-url}")
    private String accountServiceBaseUrl;

    @Override
    public AccountTransactionResponse applyTransaction(
            String accountId,
            AccountTransactionRequest request
    ) {
        String traceId = MDC.get(TracingConstants.TRACE_ID_MDC_KEY);

        return circuitBreakerFactory.create("accountServiceCircuitBreaker")
                .run(
                        () -> callAccountService(accountId, request, traceId),
                        throwable -> handleAccountServiceFailure(accountId, request, throwable)
                );
    }

    @Override
    public AccountBalanceResponse getBalance(String accountId) {
        String traceId = MDC.get(TracingConstants.TRACE_ID_MDC_KEY);

        return circuitBreakerFactory.create("accountServiceCircuitBreaker")
                .run(
                        () -> callAccountServiceBalance(accountId, traceId),
                        throwable -> handleBalanceFailure(accountId, throwable)
                );
    }

    private AccountTransactionResponse callAccountService(
            String accountId,
            AccountTransactionRequest request,
            String traceId
    ) {
        log.info("Calling Account Service accountId={} eventId={}", accountId, request.getEventId());

        String url = accountServiceBaseUrl + "/accounts/" + accountId + "/transactions";

        return serviceBroker.post(url, request, AccountTransactionResponse.class, traceId);
    }

    private AccountTransactionResponse handleAccountServiceFailure(
            String accountId,
            AccountTransactionRequest request,
            Throwable throwable
    ) {
        eventMetricsService.incrementAccountServiceFailure();

        log.error("Account Service unavailable accountId={} eventId={} reason={}",
                accountId, request.getEventId(), throwable.getMessage());

        throw new AccountServiceUnavailableException("Account Service is currently unavailable");
    }

    private AccountBalanceResponse callAccountServiceBalance(
            String accountId,
            String traceId
    ) {
        log.info("Calling Account Service balance accountId={}", accountId);

        String url = accountServiceBaseUrl + "/accounts/" + accountId + "/balance";

        return serviceBroker.get(url, AccountBalanceResponse.class, traceId);
    }

    private AccountBalanceResponse handleBalanceFailure(
            String accountId,
            Throwable throwable
    ) {
        eventMetricsService.incrementAccountServiceFailure();

        log.error("Account Service balance call failed accountId={} reason={}",
                accountId, throwable.getMessage());

        throw new AccountServiceUnavailableException("Account Service is currently unreachable for balance query");
    }
}
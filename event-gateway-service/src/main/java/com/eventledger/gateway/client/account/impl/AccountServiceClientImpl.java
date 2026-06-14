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

/**
 * Description: AccountServiceClientImpl.java is the HTTP client implementation for all
 * outbound calls from the Event Gateway Service to the Account Service. It wraps every
 * call in a Resilience4j circuit breaker, propagates the distributed trace ID from MDC,
 * delegates HTTP execution to ServiceBroker, increments failure metrics on errors, and
 * throws AccountServiceUnavailableException when the Account Service is unreachable or
 * the circuit is open.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountServiceClientImpl implements AccountServiceClient {

    private final ServiceBroker serviceBroker;

    private final EventMetricsService eventMetricsService;

    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${account.service.base-url}")
    private String accountServiceBaseUrl;

    /**
     * Sends a transaction request to the Account Service wrapped in a circuit breaker.
     * Reads the current trace ID from MDC and forwards it with the request.
     *
     * @param accountId the target account ID
     * @param request   the transaction details to apply
     * @return AccountTransactionResponse from the Account Service
     * @throws com.eventledger.gateway.common.exception.AccountServiceUnavailableException
     *         if the call fails or the circuit breaker is open
     */
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

    /**
     * Retrieves the current balance for an account from the Account Service wrapped in a
     * circuit breaker. Reads the current trace ID from MDC and forwards it with the request.
     *
     * @param accountId the account whose balance is requested
     * @return AccountBalanceResponse containing the accountId and computed balance
     * @throws com.eventledger.gateway.common.exception.AccountServiceUnavailableException
     *         if the call fails or the circuit breaker is open
     */
    @Override
    public AccountBalanceResponse getBalance(String accountId) {
        String traceId = MDC.get(TracingConstants.TRACE_ID_MDC_KEY);

        return circuitBreakerFactory.create("accountServiceCircuitBreaker")
                .run(
                        () -> callAccountServiceBalance(accountId, traceId),
                        throwable -> handleBalanceFailure(accountId, throwable)
                );
    }

    /**
     * Executes the HTTP POST to the Account Service transactions endpoint.
     *
     * @param accountId the target account ID used to build the URL
     * @param request   the transaction payload to send
     * @param traceId   the trace ID to attach as X-Trace-Id header
     * @return AccountTransactionResponse deserialized from the Account Service response
     */
    private AccountTransactionResponse callAccountService(
            String accountId,
            AccountTransactionRequest request,
            String traceId
    ) {
        log.info("Calling Account Service accountId={} eventId={}", accountId, request.getEventId());

        String url = accountServiceBaseUrl + "/accounts/" + accountId + "/transactions";

        return serviceBroker.post(url, request, AccountTransactionResponse.class, traceId);
    }

    /**
     * Circuit breaker fallback for failed transaction calls. Increments the failure metric,
     * logs the error, and throws AccountServiceUnavailableException.
     *
     * @param accountId the account ID that was targeted
     * @param request   the transaction request that failed
     * @param throwable the underlying cause of the failure
     * @return never returns — always throws AccountServiceUnavailableException
     */
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

    /**
     * Executes the HTTP GET to the Account Service balance endpoint.
     *
     * @param accountId the account whose balance is requested
     * @param traceId   the trace ID to attach as X-Trace-Id header
     * @return AccountBalanceResponse deserialized from the Account Service response
     */
    private AccountBalanceResponse callAccountServiceBalance(
            String accountId,
            String traceId
    ) {
        log.info("Calling Account Service balance accountId={}", accountId);

        String url = accountServiceBaseUrl + "/accounts/" + accountId + "/balance";

        return serviceBroker.get(url, AccountBalanceResponse.class, traceId);
    }

    /**
     * Circuit breaker fallback for failed balance calls. Increments the failure metric,
     * logs the error, and throws AccountServiceUnavailableException.
     *
     * @param accountId the account ID that was targeted
     * @param throwable the underlying cause of the failure
     * @return never returns — always throws AccountServiceUnavailableException
     */
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
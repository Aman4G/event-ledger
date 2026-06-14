package com.eventledger.gateway.common.utils;

import com.eventledger.gateway.common.constants.TracingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Description: ServiceBroker.java is a generic HTTP utility component in the Event Gateway
 * Service that centralizes outbound WebClient calls to downstream services. It handles POST
 * and GET requests, conditionally attaches the X-Trace-Id header when a trace ID is present,
 * maps HTTP error responses to exceptions, and blocks on the reactive response to integrate
 * with the servlet-based gateway.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceBroker {

    private final WebClient webClient;

    /**
     * Executes an HTTP POST request to the given URL, serializing the requestBody as JSON.
     * Attaches the X-Trace-Id header if a non-blank traceId is provided. Throws a reactive
     * WebClientResponseException for any 4xx or 5xx response.
     *
     * @param url          the target URL to POST to
     * @param requestBody  the request payload to serialize and send
     * @param responseType the expected response body class
     * @param traceId      the trace ID to propagate via X-Trace-Id header, may be null
     * @param <T>          the type of the request body
     * @param <R>          the type of the response body
     * @return the deserialized response body
     */
    public <T, R> R post(String url, T requestBody, Class<R> responseType, String traceId) {
        log.info("Executing POST request url={}", url);

        WebClient.RequestBodySpec requestSpec = webClient.post()
                .uri(url);

        addTraceIdHeader(requestSpec, traceId);

        return requestSpec
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        ClientResponse::createException
                )
                .bodyToMono(responseType)
                .block();
    }

    /**
     * Executes an HTTP GET request to the given URL. Attaches the X-Trace-Id header if a
     * non-blank traceId is provided. Throws a reactive WebClientResponseException for any
     * 4xx or 5xx response.
     *
     * @param url          the target URL to GET
     * @param responseType the expected response body class
     * @param traceId      the trace ID to propagate via X-Trace-Id header, may be null
     * @param <R>          the type of the response body
     * @return the deserialized response body
     */
    public <R> R get(String url, Class<R> responseType, String traceId) {
        log.info("Executing GET request url={}", url);

        WebClient.RequestHeadersSpec<?> requestSpec = webClient.get()
                .uri(url);

        requestSpec = addTraceIdHeader(requestSpec, traceId);

        return requestSpec
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        ClientResponse::createException
                )
                .bodyToMono(responseType)
                .block();
    }

    /**
     * Conditionally adds the X-Trace-Id header to the outbound request if the traceId is
     * non-blank. Returns the original request spec unchanged if no traceId is present.
     *
     * @param requestSpec the current request spec to potentially add the header to
     * @param traceId     the trace ID value, may be null or blank
     * @return the request spec with the header added, or the original spec if traceId is absent
     */
    private WebClient.RequestHeadersSpec<?> addTraceIdHeader(
            WebClient.RequestHeadersSpec<?> requestSpec,
            String traceId
    ) {
        if (StringUtils.hasText(traceId)) {
            return requestSpec.header(TracingConstants.TRACE_ID_HEADER, traceId);
        }

        return requestSpec;
    }
}
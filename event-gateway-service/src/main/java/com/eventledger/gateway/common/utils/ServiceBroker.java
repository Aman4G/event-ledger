package com.eventledger.gateway.common.utils;

import com.eventledger.gateway.common.constants.TracingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceBroker {

    private final WebClient webClient;

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
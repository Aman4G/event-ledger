package com.eventledger.gateway.config;

import com.eventledger.gateway.common.constants.TracingConstants;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Description: WebClientConfig.java configures the reactive WebClient bean used by the
 * Event Gateway Service for all outbound HTTP calls to the Account Service. It registers
 * a filter that reads the current trace ID from SLF4J MDC and automatically injects it
 * as an X-Trace-Id header on every outbound request, enabling end-to-end distributed
 * tracing across service boundaries.
 */
@Configuration
public class WebClientConfig {

    /**
     * Creates and configures the WebClient bean with a trace propagation filter. The filter
     * reads the traceId from MDC on each request and adds it as the X-Trace-Id header if present.
     *
     * @param builder the Spring-provided WebClient.Builder to configure
     * @return the configured WebClient instance
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {

        return builder
                .filter((request, next) -> {

                    String traceId = MDC.get(TracingConstants.TRACE_ID_MDC_KEY);

                    ClientRequest.Builder requestBuilder = ClientRequest.from(request);

                    if (StringUtils.hasText(traceId)) {
                        requestBuilder.header(
                                TracingConstants.TRACE_ID_HEADER,
                                traceId
                        );
                    }

                    return next.exchange(requestBuilder.build());
                })
                .build();
    }
}
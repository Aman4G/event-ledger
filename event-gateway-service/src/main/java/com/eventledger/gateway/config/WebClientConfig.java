package com.eventledger.gateway.config;

import com.eventledger.gateway.common.constants.TracingConstants;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

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
package com.eventledger.gateway.common.filter;

import com.eventledger.gateway.common.constants.TracingConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String traceId = request.getHeader(TracingConstants.TRACE_ID_HEADER);

        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(TracingConstants.TRACE_ID_MDC_KEY, traceId);

        response.setHeader(TracingConstants.TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TracingConstants.TRACE_ID_MDC_KEY);
        }
    }
}
package com.eventledger.account.common.filter;

import com.eventledger.account.common.constants.TracingConstants;
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

/**
 * Description: TraceIdFilter.java is a servlet filter in the Account Service that runs on
 * every incoming HTTP request to establish distributed trace context. It extracts the
 * X-Trace-Id header propagated by the Event Gateway Service, or generates a new UUID if
 * absent. The trace ID is stored in SLF4J MDC for structured log output and echoed back
 * in the HTTP response header. MDC is cleaned up after each request.
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * Intercepts each request to extract or generate a trace ID, stores it in MDC, and
     * echoes it back in the response header. Guarantees MDC cleanup in the finally block.
     *
     * @param request     the incoming HTTP servlet request
     * @param response    the outgoing HTTP servlet response
     * @param filterChain the remaining filter chain to invoke
     * @throws ServletException if the filter chain throws a servlet error
     * @throws IOException      if an I/O error occurs during filtering
     */
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

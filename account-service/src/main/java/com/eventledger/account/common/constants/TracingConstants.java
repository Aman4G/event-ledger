package com.eventledger.account.common.constants;

public final class TracingConstants {

    private TracingConstants() {
    }

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
}
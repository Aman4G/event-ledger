package com.eventledger.account.interfaces.health.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class HealthResponse {

    private String service;

    private String status;

    private Instant timestamp;

    private Map<String, Object> diagnostics;
}

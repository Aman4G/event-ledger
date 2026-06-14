package com.eventledger.account.common.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ErrorResponse {

    private Instant timestamp;

    private int status;

    private String error;

    private String message;

    private String path;

    private String traceId;

    private List<String> validationErrors;
}
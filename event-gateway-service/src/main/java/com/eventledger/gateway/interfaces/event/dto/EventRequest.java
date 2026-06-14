package com.eventledger.gateway.interfaces.event.dto;

import com.eventledger.gateway.common.enums.EventType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
public class EventRequest {

    @NotBlank(message = "eventId is required")
    private String eventId;

    @NotBlank(message = "accountId is required")
    private String accountId;

    @NotNull(message = "type is required")
    private EventType type;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    private String currency;

    @NotNull(message = "eventTimestamp is required")
    private Instant eventTimestamp;

    private Map<String, Object> metadata;
}

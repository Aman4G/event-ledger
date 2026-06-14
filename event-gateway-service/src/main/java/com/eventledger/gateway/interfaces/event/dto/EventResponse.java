package com.eventledger.gateway.interfaces.event.dto;

import com.eventledger.gateway.common.enums.EventType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private String eventId;
    private String accountId;
    private EventType type;
    private BigDecimal amount;
    private String currency;
    private Instant eventTimestamp;
    private Map<String, Object> metadata;
    private String status;
}

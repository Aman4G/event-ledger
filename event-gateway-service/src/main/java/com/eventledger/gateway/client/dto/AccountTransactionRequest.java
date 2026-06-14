package com.eventledger.gateway.client.dto;

import com.eventledger.gateway.common.enums.EventType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTransactionRequest {

    private String eventId;
    private String accountId;
    private EventType type;
    private BigDecimal amount;
    private String currency;
    private Instant eventTimestamp;
}
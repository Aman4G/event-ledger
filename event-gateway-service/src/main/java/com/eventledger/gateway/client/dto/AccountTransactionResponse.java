package com.eventledger.gateway.client.dto;

import com.eventledger.gateway.common.enums.EventType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class AccountTransactionResponse {

    private String eventId;
    private String accountId;
    private EventType type;
    private BigDecimal amount;
    private String currency;
    private Instant eventTimestamp;
    private String status;
}

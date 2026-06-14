package com.eventledger.account.interfaces.transaction.dto;

import com.eventledger.account.common.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class TransactionResponse {

    private String eventId;

    private String accountId;

    private TransactionType type;

    private BigDecimal amount;

    private String currency;

    private Instant eventTimestamp;

    private String status;
}
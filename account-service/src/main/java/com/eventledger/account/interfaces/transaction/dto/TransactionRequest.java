package com.eventledger.account.interfaces.transaction.dto;

import com.eventledger.account.common.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class TransactionRequest {

    @NotBlank
    private String eventId;

    @NotBlank
    private String accountId;

    @NotNull
    private TransactionType type;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private Instant eventTimestamp;

}
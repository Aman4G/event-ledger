package com.eventledger.account.interfaces.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class BalanceResponse {

    private String accountId;

    private BigDecimal balance;

}
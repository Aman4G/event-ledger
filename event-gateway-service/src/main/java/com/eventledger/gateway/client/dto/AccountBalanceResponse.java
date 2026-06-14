package com.eventledger.gateway.client.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountBalanceResponse {

    private String accountId;

    private BigDecimal balance;
}

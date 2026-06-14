package com.eventledger.account.interfaces.ledger.dto;

import com.eventledger.account.interfaces.transaction.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class AccountResponse {

    private String accountId;

    private List<Transaction> transactions;

}
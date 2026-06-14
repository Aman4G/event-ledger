package com.eventledger.account.interfaces.transaction.service;

import com.eventledger.account.interfaces.transaction.dto.TransactionRequest;
import com.eventledger.account.interfaces.transaction.dto.TransactionResponse;

import java.math.BigDecimal;

public interface TransactionService {

    TransactionResponse applyTransaction(String accountId, TransactionRequest request);

    BigDecimal calculateBalance(String accountId);
}
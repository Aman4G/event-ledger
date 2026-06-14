package com.eventledger.account.interfaces.ledger;

import com.eventledger.account.interfaces.ledger.dto.AccountResponse;
import com.eventledger.account.interfaces.ledger.dto.BalanceResponse;
import com.eventledger.account.interfaces.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable String accountId
    ) {
        BalanceResponse response = ledgerService.getBalance(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountDetails(
            @PathVariable String accountId
    ) {
        AccountResponse response = ledgerService.getAccountDetails(accountId);
        return ResponseEntity.ok(response);
    }
}
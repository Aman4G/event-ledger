package com.eventledger.account.interfaces.ledger;

import com.eventledger.account.interfaces.ledger.dto.AccountResponse;
import com.eventledger.account.interfaces.ledger.dto.BalanceResponse;
import com.eventledger.account.interfaces.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Description: LedgerController.java is the REST controller in the Account Service that
 * exposes account ledger read endpoints. It provides balance retrieval and full account
 * detail queries, delegating all business logic to LedgerService. These endpoints are
 * intended for internal use by the Event Gateway Service only.
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    /**
     * Returns the current net balance for the specified account.
     *
     * @param accountId the account whose balance is requested
     * @return 200 with BalanceResponse containing accountId and net balance,
     *         404 if the account does not exist
     */
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable String accountId
    ) {
        BalanceResponse response = ledgerService.getBalance(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns full account details including all transactions ordered by eventTimestamp ascending.
     *
     * @param accountId the account whose details are requested
     * @return 200 with AccountResponse containing accountId and transaction history,
     *         404 if the account does not exist
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountDetails(
            @PathVariable String accountId
    ) {
        AccountResponse response = ledgerService.getAccountDetails(accountId);
        return ResponseEntity.ok(response);
    }
}
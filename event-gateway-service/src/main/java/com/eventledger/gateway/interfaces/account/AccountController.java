package com.eventledger.gateway.interfaces.account;

import com.eventledger.gateway.client.account.AccountServiceClient;
import com.eventledger.gateway.client.dto.AccountBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Description: AccountController.java is the REST controller in the Event Gateway Service that
 * proxies account balance requests to the Account Service. It exposes the balance endpoint to
 * external clients and delegates retrieval to the AccountServiceClient, which wraps the call
 * with a circuit breaker. Returns 503 if the Account Service is unavailable.
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountServiceClient accountServiceClient;

    /**
     * Proxies a balance retrieval request to the Account Service for the given account.
     *
     * @param accountId the account whose current balance is requested
     * @return 200 with AccountBalanceResponse containing the current balance,
     *         or 503 if the Account Service is unavailable
     */
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(
            @PathVariable String accountId
    ) {
        return ResponseEntity.ok(accountServiceClient.getBalance(accountId));
    }
}
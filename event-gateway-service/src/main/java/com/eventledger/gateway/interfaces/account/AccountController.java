package com.eventledger.gateway.interfaces.account;

import com.eventledger.gateway.client.account.AccountServiceClient;
import com.eventledger.gateway.client.dto.AccountBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountServiceClient accountServiceClient;

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(
            @PathVariable String accountId
    ) {
        return ResponseEntity.ok(accountServiceClient.getBalance(accountId));
    }
}
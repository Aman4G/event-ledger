package com.eventledger.account.interfaces.transaction;

import com.eventledger.account.interfaces.transaction.dto.TransactionRequest;
import com.eventledger.account.interfaces.transaction.dto.TransactionResponse;
import com.eventledger.account.interfaces.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<TransactionResponse> applyTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody TransactionRequest request
    ) {
        TransactionResponse response = transactionService.applyTransaction(accountId, request);

        HttpStatus httpStatus = "DUPLICATE_IGNORED".equals(response.getStatus())
                ? HttpStatus.OK
                : HttpStatus.CREATED;

        return ResponseEntity.status(httpStatus).body(response);
    }
}
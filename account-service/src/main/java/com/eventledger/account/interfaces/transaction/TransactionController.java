package com.eventledger.account.interfaces.transaction;

import com.eventledger.account.interfaces.transaction.dto.TransactionRequest;
import com.eventledger.account.interfaces.transaction.dto.TransactionResponse;
import com.eventledger.account.interfaces.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Description: TransactionController.java is the REST controller in the Account Service
 * that handles incoming transaction requests from the Event Gateway Service. It exposes
 * the POST /accounts/{accountId}/transactions endpoint, delegates processing to
 * TransactionService, and maps the service response status to the appropriate HTTP status
 * code — 201 for newly applied transactions and 200 for duplicates.
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Applies a transaction to the specified account. Returns 201 if the transaction was
     * newly applied, or 200 if the eventId was already processed (idempotency).
     *
     * @param accountId the target account ID from the URL path
     * @param request   the validated transaction payload from the request body
     * @return 201 with TransactionResponse on new transaction, 200 on duplicate
     */
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
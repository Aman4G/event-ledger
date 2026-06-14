package com.eventledger.account.common.exception;

public class AccountMismatchException extends RuntimeException {

    public AccountMismatchException(String message) {
        super(message);
    }
}
package com.eventledger.gateway.common.exception;

public class AccountServiceUnavailableException extends RuntimeException {

    public AccountServiceUnavailableException(String message) {
        super(message);
    }
}
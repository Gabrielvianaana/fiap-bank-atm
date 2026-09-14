package com.fiap.bank.atm.application.exception;

/** Contrato de falha: a conta esta bloqueada por seguranca. */
public class AccountBlockedException extends AtmOperationException {

    public AccountBlockedException(String message) {
        super(message);
    }

    public AccountBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}

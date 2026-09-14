package com.fiap.bank.atm.application.exception;

/** Contrato de falha: credencial (PIN) invalida ou conta inexistente. */
public class InvalidPinException extends AtmOperationException {

    public InvalidPinException(String message) {
        super(message);
    }

    public InvalidPinException(String message, Throwable cause) {
        super(message, cause);
    }
}

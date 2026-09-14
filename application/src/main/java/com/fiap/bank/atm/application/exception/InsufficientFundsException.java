package com.fiap.bank.atm.application.exception;

/** Contrato de falha: saldo insuficiente para concluir a operacao. */
public class InsufficientFundsException extends AtmOperationException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}

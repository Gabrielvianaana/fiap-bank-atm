package com.fiap.bank.atm.application.exception;

/** Contrato de falha: limite diario de saque excedido. */
public class DailyLimitExceededException extends AtmOperationException {

    public DailyLimitExceededException(String message) {
        super(message);
    }

    public DailyLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.fiap.bank.atm.application.exception;

/**
 * Contrato de falha: conta nao localizada na base relacional.
 *
 * <p>
 * Estende {@link IllegalArgumentException} de proposito: e o resultado natural
 * de um {@code Optional.empty()} devolvido pelo repositorio quando o numero de
 * conta informado no terminal nao existe.
 * </p>
 */
public class AccountNotFoundException extends IllegalArgumentException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}

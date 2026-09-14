package com.fiap.bank.atm.infrastructure.persistence;

/**
 * Falha tecnica de acesso a dados.
 *
 * <p>
 * Envelopa {@code SQLException} (excecao verificada) em uma excecao nao
 * verificada, impedindo que detalhes de JDBC contaminem as assinaturas dos
 * contratos ditados pelo dominio.
 * </p>
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}

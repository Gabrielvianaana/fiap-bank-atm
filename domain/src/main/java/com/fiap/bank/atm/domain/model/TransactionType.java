package com.fiap.bank.atm.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tipos de movimentacao financeira suportados pelo terminal, espelhando a
 * coluna {@code type VARCHAR(20)} da tabela {@code tb_transaction} (Anexo 7.2).
 */
public enum TransactionType {

    WITHDRAWAL("Saque"),
    DEPOSIT("Depósito"),
    TRANSFER_OUT("Transf. Enviada"),
    TRANSFER_IN("Transf. Recebida");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Converte o texto persistido no banco em enum, sem jamais devolver
     * {@code null}: o resultado vem envelopado em {@link Optional} e a busca
     * e resolvida com <b>Streams API</b>.
     */
    public static Optional<TransactionType> fromDatabase(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .flatMap(raw -> Arrays.stream(values())
                        .filter(type -> type.name().equalsIgnoreCase(raw))
                        .findFirst());
    }
}

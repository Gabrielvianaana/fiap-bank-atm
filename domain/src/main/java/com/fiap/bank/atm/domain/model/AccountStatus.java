package com.fiap.bank.atm.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Situacao cadastral da conta, espelhando a coluna {@code status VARCHAR(20)}
 * da tabela {@code tb_account} (Anexo 7.2).
 */
public enum AccountStatus {

    ACTIVE("Ativa"),
    BLOCKED("Bloqueada");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Converte o texto vindo do banco de dados em enum.
     *
     * <p>
     * Busca resolvida via <b>Streams API</b> e devolvida em {@link Optional},
     * eliminando qualquer retorno nulo na conversao.
     * </p>
     */
    public static Optional<AccountStatus> fromDatabase(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .flatMap(raw -> Arrays.stream(values())
                        .filter(status -> status.name().equalsIgnoreCase(raw))
                        .findFirst());
    }
}

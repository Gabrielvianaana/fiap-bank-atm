package com.fiap.bank.atm.domain.repository;

import com.fiap.bank.atm.domain.model.BaseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato generico de repositorio do FIAP Bank ATM (Fase 3).
 *
 * <p>
 * A restricao de <b>limite superior</b> {@code <T extends BaseEntity>} garante,
 * em tempo de compilacao, que apenas entidades legitimas do dominio possam
 * transitar pela camada de persistencia.
 * </p>
 *
 * <p>
 * <b>Erradicacao do null:</b> toda operacao de busca individual devolve
 * {@link Optional}, obrigando a camada orquestradora a tratar ativamente a
 * presenca ou ausencia do dado. Nenhuma implementacao pode retornar
 * {@code null}.
 * </p>
 *
 * <p>
 * Repare que o contrato e agnostico a tecnologia: nao ha uma unica referencia a
 * JDBC, SQL ou SQLite aqui. O dominio dita as regras; a infraestrutura obedece.
 * </p>
 *
 * @param <T> tipo da entidade gerenciada, obrigatoriamente um {@link BaseEntity}
 */
public interface ATMRepository<T extends BaseEntity> {

    /**
     * Recupera uma entidade pelo identificador unico.
     *
     * @return {@link Optional} preenchido quando encontrada, vazio caso contrario
     */
    Optional<T> buscarPorId(UUID id);

    /** Persiste (insere ou atualiza) a entidade informada. */
    void salvar(T entidade);

    /** Remove a entidade associada ao identificador informado. */
    void remover(UUID id);

    /** Lista todas as entidades gerenciadas. Devolve lista vazia, nunca nula. */
    List<T> buscarTodos();
}

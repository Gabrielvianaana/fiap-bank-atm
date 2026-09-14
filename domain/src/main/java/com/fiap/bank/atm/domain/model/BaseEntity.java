package com.fiap.bank.atm.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Superclasse de toda entidade do dominio.
 *
 * <p>
 * Serve como <b>limite superior (upper bound)</b> do repositorio generico
 * {@code ATMRepository<T extends BaseEntity>}, garantindo em tempo de
 * compilacao que somente entidades legitimas do dominio possam ser
 * persistidas.
 * </p>
 */
public abstract class BaseEntity {

    private final UUID id;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Construtor de criacao: uma entidade nova nasce agora. */
    protected BaseEntity(UUID id) {
        this(id, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Construtor de <b>reconstituicao</b>: usado pela camada de infraestrutura
     * para remontar a entidade a partir de um {@code ResultSet} do banco
     * relacional, preservando as datas originais de auditoria.
     */
    protected BaseEntity(UUID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "O identificador da entidade nao pode ser nulo");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt nao pode ser nulo");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt nao pode ser nulo");
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt nao pode ser nulo");
    }

    /** Marca a entidade como alterada no instante atual. */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Identidade de entidade (DDD): duas entidades sao iguais se o ID e igual. */
    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}

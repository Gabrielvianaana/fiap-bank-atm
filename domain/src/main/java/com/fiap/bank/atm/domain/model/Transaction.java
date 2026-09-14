package com.fiap.bank.atm.domain.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro imutavel de uma movimentacao financeira vinculada a uma conta.
 *
 * <p>
 * Mapeia diretamente a tabela {@code tb_transaction} (Anexo 7.2):
 * {@code id, account_id, type, amount, created_at}.
 * </p>
 */
public final class Transaction extends BaseEntity {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final UUID accountId;
    private final TransactionType type;
    private final Money amount;
    private final String description;

    /** Cria uma transacao nova, carimbada com o instante atual. */
    public Transaction(UUID id, UUID accountId, TransactionType type, Money amount, String description) {
        this(id, accountId, LocalDateTime.now(), type, amount, description);
    }

    /**
     * Reconstitui uma transacao ja existente (carga do banco ou seed),
     * preservando o {@code created_at} original.
     */
    public Transaction(UUID id, UUID accountId, LocalDateTime timestamp, TransactionType type, Money amount,
            String description) {
        super(id, timestamp, timestamp);
        this.accountId = Objects.requireNonNull(accountId, "A transacao deve estar vinculada a uma conta");
        this.type = Objects.requireNonNull(type, "O tipo da transacao nao pode ser nulo");
        this.amount = Objects.requireNonNull(amount, "O valor da transacao nao pode ser nulo");
        this.description = Objects.requireNonNullElse(description, type.getDescription());
    }

    public UUID getAccountId() {
        return accountId;
    }

    /** Alias semantico para {@code getCreatedAt()} (coluna {@code created_at}). */
    public LocalDateTime getTimestamp() {
        return getCreatedAt();
    }

    public TransactionType getType() {
        return type;
    }

    public Money getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getFormattedTimestamp() {
        return getTimestamp().format(FORMATTER);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s (%s)", getFormattedTimestamp(), type.getDescription(), amount, description);
    }
}

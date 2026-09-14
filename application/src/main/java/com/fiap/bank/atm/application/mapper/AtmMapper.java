package com.fiap.bank.atm.application.mapper;

import com.fiap.bank.atm.application.dto.AccountInfoDTO;
import com.fiap.bank.atm.application.dto.TransactionDTO;
import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Transaction;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Tradutor entre as entidades do dominio e os contratos de transferencia
 * (Java Records) publicados para a camada de apresentacao.
 *
 * <p>
 * Este e o unico ponto do sistema autorizado a "abrir" o agregado
 * {@link Account}. Dele para fora, so trafega DTO.
 * </p>
 */
public final class AtmMapper {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private AtmMapper() {
        throw new UnsupportedOperationException("Classe utilitaria nao deve ser instanciada");
    }

    /** Converte a raiz de agregado em contrato de leitura para a tela. */
    public static AccountInfoDTO toAccountInfo(Account account) {
        return new AccountInfoDTO(
                account.getId(),
                account.getAgency(),
                account.getAccountNumber(),
                account.getBalance().getAmount(),
                account.getBalance().format(),
                account.getDailyWithdrawalLimit().getAmount(),
                account.getTotalWithdrawnToday().getAmount(),
                account.getRemainingDailyLimit().getAmount(),
                account.getRemainingDailyLimit().format(),
                account.getStatus().name(),
                account.isBlocked(),
                account.getFailedAttempts());
    }

    /** Variante segura: converte apenas se a conta estiver presente. */
    public static Optional<AccountInfoDTO> toAccountInfo(Optional<Account> account) {
        return account.map(AtmMapper::toAccountInfo);
    }

    /** Converte uma movimentacao do extrato em contrato de leitura. */
    public static TransactionDTO toTransaction(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getType().name(),
                transaction.getType().getDescription(),
                transaction.getAmount().getAmount(),
                transaction.getAmount().format(),
                transaction.getTimestamp(),
                transaction.getTimestamp().format(TIMESTAMP_FORMATTER),
                transaction.getDescription());
    }

    /**
     * Converte uma colecao de movimentacoes.
     *
     * <p>
     * Conversao resolvida com <b>Streams API</b> e method reference, no lugar
     * do antigo laco {@code for} acumulador.
     * </p>
     */
    public static List<TransactionDTO> toTransactionList(List<Transaction> transactions) {
        return transactions.stream()
                .map(AtmMapper::toTransaction)
                .toList();
    }
}

package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.AccountStatus;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacao concreta do contrato {@link AccountRepository} sobre banco
 * relacional SQLite, usando <b>exclusivamente a API nativa JDBC</b> (Fase 4).
 *
 * <h2>Compromissos tecnicos honrados aqui</h2>
 * <ul>
 * <li><b>Proibicao de ORM:</b> nenhuma linha de Hibernate, JPA ou Spring Data.
 * Todo o mapeamento objeto-relacional e escrito a mao com
 * {@link PreparedStatement} e {@link ResultSet}.</li>
 * <li><b>Protecao contra SQL Injection:</b> nao existe uma unica concatenacao
 * de String para montar SQL. Todos os comandos sao constantes estaticas com
 * marcadores {@code ?} e os valores entram apenas por metodos {@code set}.
 * A classe {@code Statement} basica jamais e usada para dados transacionais.</li>
 * <li><b>Erradicacao do null:</b> toda busca devolve {@link Optional} e toda
 * listagem devolve lista vazia quando nao ha resultado.</li>
 * <li><b>Atomicidade:</b> a gravacao do agregado (conta + lancamentos) roda em
 * uma unica transacao, com {@code commit} / {@code rollback} explicitos.</li>
 * </ul>
 */
public class AccountRepositoryJdbcImpl implements AccountRepository {

    // ------------------------------------------------------------------
    // Comandos SQL parametrizados (Anexo 7.3)
    // ------------------------------------------------------------------

    /**
     * SELECT: Buscar uma conta pelo numero da conta.
     *
     * <p>
     * Comando declarado por extenso, sem qualquer operador de concatenacao: o
     * unico ponto variavel e o marcador {@code ?}, preenchido via
     * {@code setString}.
     * </p>
     */
    private static final String SELECT_BY_NUMBER = """
            SELECT id, agency, number, balance, status, pin, daily_withdrawal_limit,
                   total_withdrawn_today, last_withdrawal_date, failed_attempts,
                   created_at, updated_at
              FROM tb_account
             WHERE number = ?
            """;

    /** SELECT: Buscar uma conta pelo identificador unico. */
    private static final String SELECT_BY_ID = """
            SELECT id, agency, number, balance, status, pin, daily_withdrawal_limit,
                   total_withdrawn_today, last_withdrawal_date, failed_attempts,
                   created_at, updated_at
              FROM tb_account
             WHERE id = ?
            """;

    /** SELECT: Listar todas as contas cadastradas. */
    private static final String SELECT_ALL = """
            SELECT id, agency, number, balance, status, pin, daily_withdrawal_limit,
                   total_withdrawn_today, last_withdrawal_date, failed_attempts,
                   created_at, updated_at
              FROM tb_account
             ORDER BY agency, number
            """;

    /** SELECT: Buscar o extrato de transacoes de uma conta especifica. */
    private static final String SELECT_TRANSACTIONS_BY_ACCOUNT = """
            SELECT id, account_id, type, amount, created_at, description
              FROM tb_transaction
             WHERE account_id = ?
             ORDER BY created_at DESC
            """;

    /** INSERT/UPDATE: Persiste a conta (upsert nativo do SQLite). */
    private static final String UPSERT_ACCOUNT = """
            INSERT INTO tb_account (id, agency, number, balance, status, pin,
                                    daily_withdrawal_limit, total_withdrawn_today,
                                    last_withdrawal_date, failed_attempts,
                                    created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                agency = excluded.agency,
                number = excluded.number,
                balance = excluded.balance,
                status = excluded.status,
                pin = excluded.pin,
                daily_withdrawal_limit = excluded.daily_withdrawal_limit,
                total_withdrawn_today = excluded.total_withdrawn_today,
                last_withdrawal_date = excluded.last_withdrawal_date,
                failed_attempts = excluded.failed_attempts,
                updated_at = excluded.updated_at
            """;

    /** INSERT: Registrar uma nova transacao (Ex: Saque ou Deposito). */
    private static final String INSERT_TRANSACTION = """
            INSERT INTO tb_transaction (id, account_id, type, amount, created_at, description)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO NOTHING
            """;

    /** DELETE: Remover os lancamentos de uma conta. */
    private static final String DELETE_TRANSACTIONS_BY_ACCOUNT = "DELETE FROM tb_transaction WHERE account_id = ?";

    /** DELETE: Remover uma conta. */
    private static final String DELETE_ACCOUNT = "DELETE FROM tb_account WHERE id = ?";

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ConnectionFactory connectionFactory;

    public AccountRepositoryJdbcImpl(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory,
                "A fabrica de conexoes e obrigatoria para o repositorio JDBC");
    }

    // ==================================================================
    // Consultas
    // ==================================================================

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return Optional.ofNullable(accountNumber)
                .flatMap(number -> queryOne(SELECT_BY_NUMBER, number));
    }

    @Override
    public Optional<Account> buscarPorId(UUID id) {
        return Optional.ofNullable(id)
                .flatMap(uuid -> queryOne(SELECT_BY_ID, uuid.toString()));
    }

    @Override
    public List<Account> buscarTodos() {
        List<Account> accounts = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                accounts.add(mapAccount(resultSet));
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao listar as contas cadastradas.", ex);
        }

        // Carrega o extrato de cada conta (Streams API no lugar de laco acumulador)
        accounts.forEach(account -> loadTransactions(account));
        return List.copyOf(accounts);
    }

    /**
     * Executa uma consulta parametrizada que retorna no maximo uma conta.
     *
     * <p>
     * O parametro entra exclusivamente por {@code setString}, jamais por
     * concatenacao - barreira definitiva contra SQL Injection.
     * </p>
     */
    private Optional<Account> queryOne(String sql, String parameter) {
        try (Connection connection = connectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, parameter);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty(); // Ausencia de dado e Optional vazio, nunca null
                }

                Account account = mapAccount(resultSet);
                loadTransactions(account);
                return Optional.of(account);
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao consultar a conta no banco de dados.", ex);
        }
    }

    /** Recupera e anexa ao agregado os lancamentos vinculados a conta. */
    private void loadTransactions(Account account) {
        try (Connection connection = connectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_TRANSACTIONS_BY_ACCOUNT)) {

            statement.setString(1, account.getId().toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    mapTransaction(resultSet).ifPresent(account::attachTransaction);
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao recuperar o extrato da conta " + account.getAccountNumber(), ex);
        }
    }

    // ==================================================================
    // Gravacao
    // ==================================================================

    @Override
    public void salvar(Account account) {
        Objects.requireNonNull(account, "A conta a ser persistida nao pode ser nula");

        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            connection.setAutoCommit(false); // Abre a transacao do agregado

            persistAccount(connection, account);
            persistTransactions(connection, account);

            connection.commit();
        } catch (SQLException ex) {
            rollback(connection);
            throw new DataAccessException("Falha ao persistir a conta " + account.getAccountNumber(), ex);
        } catch (RuntimeException ex) {
            rollback(connection);
            throw ex;
        } finally {
            connectionFactory.close(connection);
        }
    }

    private void persistAccount(Connection connection, Account account) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_ACCOUNT)) {
            statement.setString(1, account.getId().toString());
            statement.setString(2, account.getAgency());
            statement.setString(3, account.getAccountNumber());
            statement.setBigDecimal(4, account.getBalance().getAmount());
            statement.setString(5, account.getStatus().name());
            statement.setString(6, account.getPin());
            statement.setBigDecimal(7, account.getDailyWithdrawalLimit().getAmount());
            statement.setBigDecimal(8, account.getTotalWithdrawnToday().getAmount());
            statement.setString(9, account.getLastWithdrawalDate().format(DATE));
            statement.setInt(10, account.getFailedAttempts());
            statement.setString(11, account.getCreatedAt().format(TIMESTAMP));
            statement.setString(12, LocalDateTime.now().format(TIMESTAMP));

            statement.executeUpdate();
        }
    }

    /**
     * Grava os lancamentos do agregado em lote.
     *
     * <p>
     * A clausula {@code ON CONFLICT DO NOTHING} torna a operacao idempotente:
     * lancamentos ja gravados sao ignorados e apenas os novos entram.
     * </p>
     */
    private void persistTransactions(Connection connection, Account account) throws SQLException {
        List<Transaction> transactions = account.getTransactions();
        if (transactions.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(INSERT_TRANSACTION)) {
            for (Transaction transaction : transactions) {
                statement.setString(1, transaction.getId().toString());
                statement.setString(2, transaction.getAccountId().toString());
                statement.setString(3, transaction.getType().name());
                statement.setBigDecimal(4, transaction.getAmount().getAmount());
                statement.setString(5, transaction.getTimestamp().format(TIMESTAMP));
                statement.setString(6, transaction.getDescription());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    @Override
    public void remover(UUID id) {
        Objects.requireNonNull(id, "O identificador da conta a remover nao pode ser nulo");

        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement removeTransactions = connection
                    .prepareStatement(DELETE_TRANSACTIONS_BY_ACCOUNT)) {
                removeTransactions.setString(1, id.toString());
                removeTransactions.executeUpdate();
            }

            try (PreparedStatement removeAccount = connection.prepareStatement(DELETE_ACCOUNT)) {
                removeAccount.setString(1, id.toString());
                removeAccount.executeUpdate();
            }

            connection.commit();
        } catch (SQLException ex) {
            rollback(connection);
            throw new DataAccessException("Falha ao remover a conta " + id, ex);
        } finally {
            connectionFactory.close(connection);
        }
    }

    private void rollback(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Rollback best-effort: a excecao original e que deve chegar ao usuario.
        }
    }

    // ==================================================================
    // Mapeamento ResultSet -> Dominio
    // ==================================================================

    /**
     * Mapeia fielmente uma linha de {@code tb_account} para a raiz de agregado,
     * recuperando coluna a coluna atraves do {@link ResultSet}.
     */
    private Account mapAccount(ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        String agency = resultSet.getString("agency");
        String number = resultSet.getString("number");
        String pin = Optional.ofNullable(resultSet.getString("pin")).orElse("0000");

        Money balance = toMoney(resultSet.getBigDecimal("balance"));
        Money dailyLimit = toMoney(resultSet.getBigDecimal("daily_withdrawal_limit"));
        Money withdrawnToday = toMoney(resultSet.getBigDecimal("total_withdrawn_today"));

        LocalDate lastWithdrawalDate = parseDate(resultSet.getString("last_withdrawal_date"))
                .orElseGet(LocalDate::now);

        AccountStatus status = AccountStatus.fromDatabase(resultSet.getString("status"))
                .orElse(AccountStatus.ACTIVE);

        int failedAttempts = resultSet.getInt("failed_attempts");

        LocalDateTime createdAt = parseTimestamp(resultSet.getString("created_at")).orElseGet(LocalDateTime::now);
        LocalDateTime updatedAt = parseTimestamp(resultSet.getString("updated_at")).orElse(createdAt);

        return new Account(id, agency, number, pin, balance, dailyLimit, withdrawnToday,
                lastWithdrawalDate, status, failedAttempts, createdAt, updatedAt);
    }

    /**
     * Mapeia uma linha de {@code tb_transaction} para a entidade de dominio.
     *
     * <p>
     * Devolve {@link Optional#empty()} quando a linha carrega um tipo
     * desconhecido, em vez de propagar {@code null} para o agregado.
     * </p>
     */
    private Optional<Transaction> mapTransaction(ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        UUID accountId = UUID.fromString(resultSet.getString("account_id"));
        Money amount = toMoney(resultSet.getBigDecimal("amount"));
        LocalDateTime createdAt = parseTimestamp(resultSet.getString("created_at")).orElseGet(LocalDateTime::now);
        String description = Optional.ofNullable(resultSet.getString("description")).orElse("");

        return TransactionType.fromDatabase(resultSet.getString("type"))
                .map(type -> new Transaction(id, accountId, createdAt, type, amount,
                        description.isBlank() ? type.getDescription() : description));
    }

    private Money toMoney(BigDecimal value) {
        return Optional.ofNullable(value).map(Money::of).orElse(Money.ZERO);
    }

    /** Converte o texto do banco em data, tolerando nulos e formatos parciais. */
    private Optional<LocalDate> parseDate(String raw) {
        return Optional.ofNullable(raw)
                .map(String::trim)
                .filter(value -> value.length() >= 10)
                .map(value -> LocalDate.parse(value.substring(0, 10), DATE));
    }

    /**
     * Converte o texto do banco em data/hora, aceitando tanto o separador ISO
     * {@code 'T'} quanto o espaco usado pelas funcoes nativas do SQLite.
     */
    private Optional<LocalDateTime> parseTimestamp(String raw) {
        return Optional.ofNullable(raw)
                .map(String::trim)
                .filter(value -> value.length() >= 19)
                .map(value -> value.replace(' ', 'T').substring(0, 19))
                .map(value -> LocalDateTime.parse(value, TIMESTAMP));
    }
}

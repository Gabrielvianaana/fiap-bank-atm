package com.fiap.bank.atm.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Fabrica de conexoes com o banco relacional SQLite (Fase 4).
 *
 * <p>
 * Unica classe do sistema autorizada a abrir e encerrar conexoes JDBC.
 * Centralizar essa responsabilidade evita vazamento de recursos e mantem o
 * repositorio focado apenas em mapeamento de dados.
 * </p>
 *
 * <p>
 * <b>Sem ORM:</b> nao ha Hibernate, JPA ou Spring Data em nenhum ponto. A
 * conexao e obtida diretamente do {@link DriverManager} da API nativa do Java.
 * </p>
 */
public class ConnectionFactory implements AutoCloseable {

    private static final String DEFAULT_DATABASE_FILE = "fiap-bank-atm.db";
    private static final String JDBC_PREFIX = "jdbc:sqlite:";

    private final String jdbcUrl;

    /** Cria a fabrica apontando para o arquivo padrao na raiz de execucao. */
    public ConnectionFactory() {
        this(Paths.get(DEFAULT_DATABASE_FILE).toAbsolutePath());
    }

    /** Cria a fabrica apontando para um arquivo especifico de banco de dados. */
    public ConnectionFactory(Path databaseFile) {
        this.jdbcUrl = JDBC_PREFIX + databaseFile.toString();
        loadDriver();
    }

    /** Construtor destinado a testes: aceita uma URL JDBC arbitraria. */
    protected ConnectionFactory(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        loadDriver();
    }

    private void loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new DataAccessException(
                    "Driver JDBC do SQLite nao encontrado no classpath. Verifique a dependencia org.xerial:sqlite-jdbc.",
                    ex);
        }
    }

    /**
     * Fornece uma conexao pronta para uso, com integridade referencial ligada.
     *
     * <p>
     * O consumidor deve encerra-la sempre via {@code try-with-resources}.
     * </p>
     */
    public Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            enableForeignKeys(connection);
            return connection;
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao abrir conexao com o banco de dados do FIAP Bank.", ex);
        }
    }

    private void enableForeignKeys(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }

    /**
     * Encerra uma conexao de forma segura, silenciando falhas de fechamento
     * para nao mascarar a excecao original da rotina transacional.
     */
    public void close(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // Encerramento best-effort: nada a fazer alem de liberar o recurso.
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Implementacao de {@link AutoCloseable} para uso da propria fabrica em
     * blocos {@code try-with-resources}. O SQLite e baseado em arquivo e nao
     * mantem pool aberto, portanto nao ha recurso residual a liberar aqui.
     */
    @Override
    public void close() {
        // Sem pool de conexoes: nada a encerrar no nivel da fabrica.
    }
}

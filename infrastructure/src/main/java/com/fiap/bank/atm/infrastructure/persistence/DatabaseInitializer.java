package com.fiap.bank.atm.infrastructure.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Prepara o banco relacional na inicializacao do terminal.
 *
 * <p>
 * Executa o DDL ({@code db/schema.sql}) e a carga inicial
 * ({@code db/data.sql}) descritos no Anexo 7.2. Ambos os scripts sao
 * idempotentes ({@code CREATE TABLE IF NOT EXISTS} e
 * {@code ON CONFLICT DO NOTHING}), portanto reiniciar a aplicacao preserva
 * integralmente os dados ja gravados - exatamente o problema de volatilidade
 * apontado na auditoria do codigo legado.
 * </p>
 */
public class DatabaseInitializer {

    private static final String SCHEMA_SCRIPT = "db/schema.sql";
    private static final String DATA_SCRIPT = "db/data.sql";

    private final ConnectionFactory connectionFactory;

    public DatabaseInitializer(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /** Cria as tabelas e aplica a carga inicial de contas de teste. */
    public void initialize() {
        try (Connection connection = connectionFactory.getConnection()) {
            runScript(connection, SCHEMA_SCRIPT);
            runScript(connection, DATA_SCRIPT);
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao inicializar o banco de dados do FIAP Bank.", ex);
        }
    }

    private void runScript(Connection connection, String resourceName) {
        List<String> commands = splitStatements(readResource(resourceName));

        try (Statement statement = connection.createStatement()) {
            for (String command : commands) {
                statement.execute(command);
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao executar o script SQL '" + resourceName + "'.", ex);
        }
    }

    /**
     * Le o script do classpath.
     *
     * <p>
     * Leitura resolvida com <b>Streams</b> de linhas, sem lacos manuais.
     * </p>
     */
    private String readResource(String resourceName) {
        InputStream input = Optional
                .ofNullable(getClass().getClassLoader().getResourceAsStream(resourceName))
                .orElseThrow(() -> new DataAccessException(
                        "Script SQL '" + resourceName + "' nao localizado no classpath."));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException ex) {
            throw new DataAccessException("Falha ao ler o script SQL '" + resourceName + "'.", ex);
        }
    }

    /**
     * Quebra o script em comandos individuais, descartando comentarios e
     * linhas em branco.
     *
     * <p>
     * Processamento feito com <b>Streams API</b> (filtros encadeados), no
     * lugar de lacos imperativos.
     * </p>
     */
    private List<String> splitStatements(String script) {
        String withoutComments = Arrays.stream(script.split("\\R"))
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));

        return Arrays.stream(withoutComments.split(";"))
                .map(String::trim)
                .filter(command -> !command.isEmpty())
                .toList();
    }
}

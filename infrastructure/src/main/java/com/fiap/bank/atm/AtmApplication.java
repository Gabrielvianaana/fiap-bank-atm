package com.fiap.bank.atm;

import com.fiap.bank.atm.application.service.AtmService;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.persistence.AccountRepositoryJdbcImpl;
import com.fiap.bank.atm.infrastructure.persistence.ConnectionFactory;
import com.fiap.bank.atm.infrastructure.persistence.DatabaseInitializer;
import com.fiap.bank.atm.presentation.AtmFrame;

import javax.swing.SwingUtilities;

/**
 * Ponto de entrada e <b>Composition Root</b> do FIAP Bank ATM.
 *
 * <p>
 * Esta e a unica classe do sistema que conhece simultaneamente todas as
 * camadas. Ela monta o grafo de objetos - infraestrutura, aplicacao e
 * apresentacao - e injeta as dependencias de fora para dentro.
 * </p>
 *
 * <p>
 * <b>Por que mora no modulo 'infrastructure'?</b> Porque a regra inviolavel
 * nº 2 proibe o POM da apresentacao de declarar dependencia com
 * 'infrastructure'. O wiring precisa viver no modulo mais externo do sistema,
 * de modo que a tela Swing continue 100% cega a JDBC, SQLite e a qualquer
 * detalhe de persistencia.
 * </p>
 *
 * <p>
 * Repare na ordem da montagem: a apresentacao recebe apenas um
 * {@link AtmService}. Trocar o SQLite por PostgreSQL, Oracle ou qualquer outro
 * banco exigiria alterar unicamente as tres linhas abaixo - nenhuma linha de
 * Swing.
 * </p>
 */
public class AtmApplication {

    public static void main(String[] args) {
        // -------------------------------------------------------------
        // 1) INFRAESTRUTURA: fabrica de conexoes, schema e carga inicial
        // -------------------------------------------------------------
        ConnectionFactory connectionFactory = new ConnectionFactory();
        new DatabaseInitializer(connectionFactory).initialize();

        AccountRepository accountRepository = new AccountRepositoryJdbcImpl(connectionFactory);

        // -------------------------------------------------------------
        // 2) APLICACAO: orquestrador injetado com o contrato do dominio
        // -------------------------------------------------------------
        AtmService atmService = new AtmService(accountRepository);

        // -------------------------------------------------------------
        // 3) APRESENTACAO: tela Swing plugada ao motor apenas via DTOs,
        //    inicializada com seguranca na Event Dispatch Thread (EDT)
        // -------------------------------------------------------------
        SwingUtilities.invokeLater(() -> {
            AtmFrame mainFrame = new AtmFrame(atmService);
            mainFrame.setVisible(true);
        });
    }
}

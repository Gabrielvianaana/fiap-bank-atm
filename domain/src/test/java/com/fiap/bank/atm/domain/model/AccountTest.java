package com.fiap.bank.atm.domain.model;

import com.fiap.bank.atm.domain.exception.AccountBlockedException;
import com.fiap.bank.atm.domain.exception.DailyLimitExceededException;
import com.fiap.bank.atm.domain.exception.InsufficientFundsException;
import com.fiap.bank.atm.domain.exception.InvalidPinException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes das regras de negocio da raiz de agregado, executados sem banco de
 * dados e sem interface grafica - prova pratica do isolamento do dominio.
 */
class AccountTest {

    private Account novaConta() {
        return new Account(UUID.randomUUID(), "0001", "12345", "1234",
                Money.of(1000.00), Money.of(500.00));
    }

    @Test
    @DisplayName("Saque debita o saldo e registra a movimentacao no extrato")
    void saqueDebitaSaldo() {
        Account conta = novaConta();

        conta.withdraw(Money.of(200.00));

        assertEquals(Money.of(800.00), conta.getBalance());
        assertEquals(1, conta.getTransactions().size());
        assertEquals(TransactionType.WITHDRAWAL, conta.getTransactions().get(0).getType());
    }

    @Test
    @DisplayName("Saque acima do saldo dispara InsufficientFundsException")
    void saqueSemSaldo() {
        Account conta = novaConta();

        assertThrows(InsufficientFundsException.class, () -> conta.withdraw(Money.of(5000.00)));
    }

    @Test
    @DisplayName("Saque acima do limite diario dispara DailyLimitExceededException")
    void saqueAcimaDoLimiteDiario() {
        Account conta = novaConta();

        assertThrows(DailyLimitExceededException.class, () -> conta.withdraw(Money.of(700.00)));
    }

    @Test
    @DisplayName("Deposito credita o saldo e registra a movimentacao")
    void depositoCreditaSaldo() {
        Account conta = novaConta();

        conta.deposit(Money.of(250.00));

        assertEquals(Money.of(1250.00), conta.getBalance());
        assertEquals(TransactionType.DEPOSIT, conta.getTransactions().get(0).getType());
    }

    @Test
    @DisplayName("Tres senhas incorretas bloqueiam a conta")
    void bloqueioAposTresTentativas() {
        Account conta = novaConta();

        assertThrows(InvalidPinException.class, () -> conta.authenticate("0000"));
        assertThrows(InvalidPinException.class, () -> conta.authenticate("0000"));
        assertThrows(AccountBlockedException.class, () -> conta.authenticate("0000"));

        assertTrue(conta.isBlocked());
        assertEquals(AccountStatus.BLOCKED, conta.getStatus());
    }

    @Test
    @DisplayName("Autenticacao bem-sucedida zera o contador de tentativas")
    void autenticacaoZeraTentativas() {
        Account conta = novaConta();

        assertThrows(InvalidPinException.class, () -> conta.authenticate("9999"));
        conta.authenticate("1234");

        assertEquals(0, conta.getFailedAttempts());
        assertFalse(conta.isBlocked());
    }

    @Test
    @DisplayName("Transferencia debita a origem e credita o destino")
    void transferenciaEntreContas() {
        Account origem = novaConta();
        Account destino = new Account(UUID.randomUUID(), "0001", "67890", "5678",
                Money.of(100.00), Money.of(500.00));

        origem.transfer(destino, Money.of(300.00));

        assertEquals(Money.of(700.00), origem.getBalance());
        assertEquals(Money.of(400.00), destino.getBalance());
        assertEquals(TransactionType.TRANSFER_OUT, origem.getTransactions().get(0).getType());
        assertEquals(TransactionType.TRANSFER_IN, destino.getTransactions().get(0).getType());
    }

    @Test
    @DisplayName("Transferencia para a propria conta e recusada")
    void transferenciaParaSiMesmo() {
        Account origem = novaConta();
        Account mesmaConta = new Account(UUID.randomUUID(), "0001", "12345", "1234",
                Money.of(100.00), Money.of(500.00));

        assertThrows(IllegalArgumentException.class, () -> origem.transfer(mesmaConta, Money.of(10.00)));
    }

    @Test
    @DisplayName("sumByType soma apenas o tipo pedido (Streams API)")
    void somatorioPorTipo() {
        Account conta = novaConta();
        conta.deposit(Money.of(100.00));
        conta.deposit(Money.of(50.00));
        conta.withdraw(Money.of(30.00));

        assertEquals(Money.of(150.00), conta.sumByType(TransactionType.DEPOSIT));
        assertEquals(Money.of(30.00), conta.sumByType(TransactionType.WITHDRAWAL));
    }

    @Test
    @DisplayName("getLatestTransactions devolve os mais recentes primeiro e respeita o limite")
    void extratoLimitadoEOrdenado() {
        Account conta = novaConta();
        conta.deposit(Money.of(10.00));
        conta.deposit(Money.of(20.00));
        conta.deposit(Money.of(30.00));

        assertEquals(2, conta.getLatestTransactions(2).size());
        assertTrue(conta.getLatestTransactions(3).get(0).getTimestamp()
                .isAfter(conta.getLatestTransactions(3).get(2).getTimestamp())
                || conta.getLatestTransactions(3).get(0).getTimestamp()
                        .isEqual(conta.getLatestTransactions(3).get(2).getTimestamp()));
    }

    @Test
    @DisplayName("Busca de transacao inexistente devolve Optional vazio, nunca null")
    void buscaDeTransacaoInexistente() {
        Account conta = novaConta();

        Optional<Transaction> resultado = conta.findTransaction(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Conversao de enums a partir do banco devolve Optional")
    void conversaoDeEnums() {
        assertTrue(TransactionType.fromDatabase("DEPOSIT").isPresent());
        assertTrue(TransactionType.fromDatabase("INEXISTENTE").isEmpty());
        assertTrue(AccountStatus.fromDatabase("blocked").isPresent());
        assertTrue(AccountStatus.fromDatabase(null).isEmpty());
    }

    @Test
    @DisplayName("Money mantem precisao decimal e formatacao pt-BR")
    void valorMonetarioPreciso() {
        Money soma = Money.of(0.10).plus(Money.of(0.20));

        assertEquals(Money.of(0.30), soma);
        assertTrue(soma.format().contains("0,30"));
    }
}

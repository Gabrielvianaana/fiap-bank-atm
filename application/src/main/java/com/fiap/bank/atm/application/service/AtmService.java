package com.fiap.bank.atm.application.service;

import com.fiap.bank.atm.application.dto.AccountInfoDTO;
import com.fiap.bank.atm.application.dto.OperationResultDTO;
import com.fiap.bank.atm.application.dto.TransactionDTO;
import com.fiap.bank.atm.application.exception.AccountNotFoundException;
import com.fiap.bank.atm.application.mapper.AtmMapper;
import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Servico de aplicacao: fachada unica entre a tela Swing e o motor de dominio.
 *
 * <p>
 * <b>Contrato blindado (Fase 2):</b> todo metodo publico recebe e devolve
 * exclusivamente DTOs (Java Records), {@link UUID}, {@link BigDecimal},
 * {@link String} ou wrappers. Nenhuma entidade de dominio ({@code Account},
 * {@code Transaction}, {@code Money}) atravessa esta fronteira.
 * </p>
 *
 * <p>
 * <b>Erradicacao do null (Fase 3):</b> as buscas do repositorio chegam aqui
 * como {@link Optional} e sao tratadas ativamente com
 * {@code orElseThrow} / {@code map}, nunca com comparacoes contra {@code null}.
 * </p>
 */
public class AtmService {

    private static final int DEFAULT_STATEMENT_SIZE = 5;

    private final AccountRepository accountRepository;

    /** Sessao corrente do terminal, tratada como Optional em vez de campo nulo. */
    private Optional<Account> currentAccount = Optional.empty();

    public AtmService(AccountRepository accountRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository,
                "O repositorio de contas e obrigatorio para operar o terminal");
    }

    // ------------------------------------------------------------------
    // Autenticacao e sessao
    // ------------------------------------------------------------------

    /**
     * Autentica o portador do cartao.
     *
     * @return contrato de leitura da conta autenticada
     */
    public AccountInfoDTO authenticate(String accountNumber, String pin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new com.fiap.bank.atm.application.exception.InvalidPinException(
                        "Conta não encontrada."));

        try {
            account.authenticate(pin);
            currentAccount = Optional.of(account);
            accountRepository.salvar(account);
            return AtmMapper.toAccountInfo(account);
        } catch (RuntimeException ex) {
            // Persiste o contador de tentativas / bloqueio antes de propagar a falha
            accountRepository.salvar(account);
            throw translate(ex);
        }
    }

    public void logout() {
        currentAccount = Optional.empty();
    }

    public boolean isAuthenticated() {
        return currentAccount.isPresent();
    }

    /**
     * Conta ativa na sessao do terminal, ja convertida em contrato de leitura.
     *
     * <p>
     * Devolve {@link Optional#empty()} quando nao ha sessao aberta. Nao existe
     * neste servico uma unica assinatura capaz de retornar {@code null}.
     * </p>
     */
    public Optional<AccountInfoDTO> findCurrentAccount() {
        return currentAccount.map(AtmMapper::toAccountInfo);
    }

    // ------------------------------------------------------------------
    // Operacoes financeiras
    // ------------------------------------------------------------------

    public OperationResultDTO withdraw(double amount) {
        Account account = requireSession();
        Money value = Money.of(amount);
        try {
            account.withdraw(value);
        } catch (RuntimeException ex) {
            throw translate(ex);
        }
        accountRepository.salvar(account);
        return result("WITHDRAWAL", value, account, "Saque realizado com sucesso.");
    }

    public OperationResultDTO deposit(double amount) {
        Account account = requireSession();
        Money value = Money.of(amount);
        try {
            account.deposit(value);
        } catch (RuntimeException ex) {
            throw translate(ex);
        }
        accountRepository.salvar(account);
        return result("DEPOSIT", value, account, "Depósito realizado com sucesso.");
    }

    public OperationResultDTO transfer(String targetAccountNumber, double amount) {
        Account source = requireSession();

        Account target = accountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Conta de destino não encontrada."));

        Money value = Money.of(amount);
        try {
            source.transfer(target, value);
        } catch (RuntimeException ex) {
            throw translate(ex);
        }

        accountRepository.salvar(source);
        accountRepository.salvar(target);
        return result("TRANSFER_OUT", value, source, "Transferência efetuada com sucesso.");
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /** Saldo disponivel em valor bruto. */
    public BigDecimal getBalance() {
        return requireSession().getBalance().getAmount();
    }

    /** Saldo disponivel ja formatado no padrao pt-BR. */
    public String getFormattedBalance() {
        return requireSession().getBalance().format();
    }

    /** Extrato completo, do lancamento mais recente para o mais antigo. */
    public List<TransactionDTO> getStatement() {
        return getStatement(Integer.MAX_VALUE);
    }

    /**
     * Extrato limitado aos {@code limit} lancamentos mais recentes.
     *
     * <p>
     * Ordenacao, corte e conversao resolvidos integralmente com
     * <b>Streams API</b>.
     * </p>
     */
    public List<TransactionDTO> getStatement(int limit) {
        return AtmMapper.toTransactionList(requireSession().getLatestTransactions(limit));
    }

    /** Extrato padrao do comprovante impresso (5 ultimos lancamentos). */
    public List<TransactionDTO> getReceiptStatement() {
        return getStatement(DEFAULT_STATEMENT_SIZE);
    }

    /**
     * Total movimentado na sessao por tipo de operacao.
     *
     * <p>
     * Somatorio calculado com <b>Streams API</b> dentro do agregado.
     * </p>
     */
    public BigDecimal getTotalByType(String transactionType) {
        Account account = requireSession();
        return TransactionType.fromDatabase(transactionType)
                .map(account::sumByType)
                .map(Money::getAmount)
                .orElse(BigDecimal.ZERO);
    }

    /** Verifica a existencia de uma conta sem expor o agregado. */
    public boolean accountExists(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).isPresent();
    }

    /**
     * Consulta publica de conta por numero, ja convertida em DTO.
     * Devolve {@link Optional#empty()} quando nao existir - nunca {@code null}.
     */
    public Optional<AccountInfoDTO> findAccount(String accountNumber) {
        return AtmMapper.toAccountInfo(accountRepository.findByAccountNumber(accountNumber));
    }

    /** Consulta publica de conta por identificador unico. */
    public Optional<AccountInfoDTO> findAccountById(UUID id) {
        return AtmMapper.toAccountInfo(accountRepository.buscarPorId(id));
    }

    /**
     * Lista todas as contas cadastradas em formato de contrato.
     *
     * <p>
     * Conversao via <b>Streams API</b>.
     * </p>
     */
    public List<AccountInfoDTO> listAccounts() {
        return accountRepository.buscarTodos().stream()
                .map(AtmMapper::toAccountInfo)
                .toList();
    }

    // ------------------------------------------------------------------
    // Apoio interno
    // ------------------------------------------------------------------

    /** Recupera o agregado da sessao ou falha explicitamente. */
    private Account requireSession() {
        return currentAccount.orElseThrow(
                () -> new IllegalStateException("Nenhum usuário está autenticado no momento."));
    }

    private OperationResultDTO result(String operation, Money value, Account account, String message) {
        return new OperationResultDTO(
                Boolean.TRUE,
                operation,
                value.getAmount(),
                value.format(),
                account.getBalance().getAmount(),
                account.getBalance().format(),
                message);
    }

    /**
     * Traduz as excecoes de negocio do dominio para os contratos de falha da
     * camada de aplicacao, unica linguagem de erro que a apresentacao conhece.
     */
    private RuntimeException translate(RuntimeException ex) {
        if (ex instanceof com.fiap.bank.atm.domain.exception.AccountBlockedException) {
            return new com.fiap.bank.atm.application.exception.AccountBlockedException(ex.getMessage(), ex);
        }
        if (ex instanceof com.fiap.bank.atm.domain.exception.InvalidPinException) {
            return new com.fiap.bank.atm.application.exception.InvalidPinException(ex.getMessage(), ex);
        }
        if (ex instanceof com.fiap.bank.atm.domain.exception.InsufficientFundsException) {
            return new com.fiap.bank.atm.application.exception.InsufficientFundsException(ex.getMessage(), ex);
        }
        if (ex instanceof com.fiap.bank.atm.domain.exception.DailyLimitExceededException) {
            return new com.fiap.bank.atm.application.exception.DailyLimitExceededException(ex.getMessage(), ex);
        }
        return ex;
    }
}

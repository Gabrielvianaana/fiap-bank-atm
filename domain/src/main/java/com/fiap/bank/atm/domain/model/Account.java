package com.fiap.bank.atm.domain.model;

import com.fiap.bank.atm.domain.exception.AccountBlockedException;
import com.fiap.bank.atm.domain.exception.DailyLimitExceededException;
import com.fiap.bank.atm.domain.exception.InsufficientFundsException;
import com.fiap.bank.atm.domain.exception.InvalidPinException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Raiz de agregado (Aggregate Root) do sistema bancario.
 *
 * <p>
 * Concentra TODAS as regras de negocio de autenticacao, saque, deposito e
 * transferencia. Nenhuma outra camada tem permissao para alterar seu estado
 * interno diretamente: a conta so muda atraves dos metodos de negocio
 * publicados aqui.
 * </p>
 *
 * <p>
 * Mapeia a tabela {@code tb_account} (Anexo 7.2).
 * </p>
 */
public class Account extends BaseEntity {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final Money MINIMUM_OPERATION_AMOUNT = Money.of(0.01);

    private final String agency;
    private final String accountNumber;
    private final String pin;
    private Money balance;
    private final Money dailyWithdrawalLimit;
    private Money totalWithdrawnToday;
    private LocalDate lastWithdrawalDate;
    private AccountStatus status;
    private int failedAttempts;
    private final List<Transaction> transactions;

    /** Construtor de criacao de uma conta nova. */
    public Account(UUID id, String agency, String accountNumber, String pin, Money initialBalance,
            Money dailyWithdrawalLimit) {
        super(id);
        this.agency = Objects.requireNonNull(agency, "A agencia nao pode ser nula");
        this.accountNumber = Objects.requireNonNull(accountNumber, "O numero da conta nao pode ser nulo");
        this.pin = Objects.requireNonNull(pin, "O PIN nao pode ser nulo");
        this.balance = Objects.requireNonNull(initialBalance, "O saldo inicial nao pode ser nulo");
        this.dailyWithdrawalLimit = Objects.requireNonNull(dailyWithdrawalLimit, "O limite diario nao pode ser nulo");
        this.totalWithdrawnToday = Money.ZERO;
        this.lastWithdrawalDate = LocalDate.now();
        this.status = AccountStatus.ACTIVE;
        this.failedAttempts = 0;
        this.transactions = new ArrayList<>();
    }

    /**
     * Construtor de <b>reconstituicao</b>: exclusivo da camada de
     * infraestrutura, remonta o agregado fielmente a partir das linhas
     * recuperadas do banco relacional via {@code ResultSet}.
     */
    public Account(UUID id, String agency, String accountNumber, String pin, Money balance,
            Money dailyWithdrawalLimit, Money totalWithdrawnToday, LocalDate lastWithdrawalDate,
            AccountStatus status, int failedAttempts, java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        this.agency = Objects.requireNonNull(agency, "A agencia nao pode ser nula");
        this.accountNumber = Objects.requireNonNull(accountNumber, "O numero da conta nao pode ser nulo");
        this.pin = Objects.requireNonNull(pin, "O PIN nao pode ser nulo");
        this.balance = Objects.requireNonNull(balance, "O saldo nao pode ser nulo");
        this.dailyWithdrawalLimit = Objects.requireNonNull(dailyWithdrawalLimit, "O limite diario nao pode ser nulo");
        this.totalWithdrawnToday = Objects.requireNonNullElse(totalWithdrawnToday, Money.ZERO);
        this.lastWithdrawalDate = Objects.requireNonNullElse(lastWithdrawalDate, LocalDate.now());
        this.status = Objects.requireNonNullElse(status, AccountStatus.ACTIVE);
        this.failedAttempts = failedAttempts;
        this.transactions = new ArrayList<>();
        resetDailyCounterIfNewDay();
    }

    // ------------------------------------------------------------------
    // Consultas de estado (somente leitura)
    // ------------------------------------------------------------------

    public String getAgency() {
        return agency;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * Exposto apenas para a camada de persistencia gravar a credencial.
     * A apresentacao nunca alcanca este metodo: o modulo 'presentation' nao
     * declara dependencia com 'domain'.
     */
    public String getPin() {
        return pin;
    }

    public Money getBalance() {
        return balance;
    }

    public Money getDailyWithdrawalLimit() {
        return dailyWithdrawalLimit;
    }

    public Money getTotalWithdrawnToday() {
        resetDailyCounterIfNewDay();
        return totalWithdrawnToday;
    }

    /** Saldo ainda disponivel dentro do limite diario de saque. */
    public Money getRemainingDailyLimit() {
        return dailyWithdrawalLimit.minus(getTotalWithdrawnToday());
    }

    public LocalDate getLastWithdrawalDate() {
        return lastWithdrawalDate;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public boolean isBlocked() {
        return status == AccountStatus.BLOCKED;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    /**
     * Extrato ordenado da movimentacao mais recente para a mais antiga,
     * limitado a {@code limit} lancamentos.
     *
     * <p>
     * Implementado com <b>Streams API</b>, substituindo os antigos lacos
     * {@code for} imperativos de filtragem.
     * </p>
     */
    public List<Transaction> getLatestTransactions(int limit) {
        return transactions.stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .limit(Math.max(limit, 0))
                .toList();
    }

    /**
     * Soma os valores de um determinado tipo de movimentacao.
     *
     * <p>
     * Filtro e somatorio resolvidos com <b>Streams API</b> + {@code reduce},
     * no lugar de acumuladores em laco.
     * </p>
     */
    public Money sumByType(TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount)
                .reduce(Money.ZERO, Money::plus);
    }

    /** Localiza uma transacao do extrato pelo identificador, sem devolver nulo. */
    public Optional<Transaction> findTransaction(UUID transactionId) {
        return transactions.stream()
                .filter(transaction -> transaction.getId().equals(transactionId))
                .findFirst();
    }

    // ------------------------------------------------------------------
    // Regras de negocio
    // ------------------------------------------------------------------

    public void authenticate(String pinAttempt) {
        if (isBlocked()) {
            throw new AccountBlockedException("Esta conta está bloqueada por excesso de tentativas de senha.");
        }

        if (!this.pin.equals(pinAttempt)) {
            failedAttempts++;
            touch();
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                status = AccountStatus.BLOCKED;
                throw new AccountBlockedException(
                        "Conta bloqueada após " + MAX_FAILED_ATTEMPTS + " tentativas incorretas.");
            }
            throw new InvalidPinException(
                    "Senha incorreta. Tentativa " + failedAttempts + " de " + MAX_FAILED_ATTEMPTS + ".");
        }

        failedAttempts = 0; // Zera o contador apos login bem-sucedido
        touch();
    }

    public void withdraw(Money amount) {
        ensureOperational("Operação não permitida: conta bloqueada.");
        ensurePositive(amount, "O valor do saque deve ser maior que zero.");

        if (amount.isGreaterThan(balance)) {
            throw new InsufficientFundsException(
                    "Saldo insuficiente para realizar o saque. Saldo disponível: " + balance);
        }

        resetDailyCounterIfNewDay();
        Money projectedWithdrawal = totalWithdrawnToday.plus(amount);
        if (projectedWithdrawal.isGreaterThan(dailyWithdrawalLimit)) {
            throw new DailyLimitExceededException("Limite diário de saque excedido. Limite restante hoje: "
                    + dailyWithdrawalLimit.minus(totalWithdrawnToday));
        }

        balance = balance.minus(amount);
        totalWithdrawnToday = projectedWithdrawal;
        lastWithdrawalDate = LocalDate.now();

        register(TransactionType.WITHDRAWAL, amount, "Saque eletrônico");
    }

    public void deposit(Money amount) {
        ensureOperational("Operação não permitida: conta bloqueada.");
        ensurePositive(amount, "O valor do depósito deve ser maior que zero.");

        balance = balance.plus(amount);

        register(TransactionType.DEPOSIT, amount, "Depósito em dinheiro");
    }

    public void transfer(Account targetAccount, Money amount) {
        Objects.requireNonNull(targetAccount, "A conta de destino nao pode ser nula");
        ensureOperational("Operação não permitida: conta de origem bloqueada.");

        if (targetAccount.isBlocked()) {
            throw new AccountBlockedException("Operação não permitida: conta de destino está bloqueada.");
        }

        ensurePositive(amount, "O valor da transferência deve ser maior que zero.");

        if (amount.isGreaterThan(balance)) {
            throw new InsufficientFundsException("Saldo insuficiente para transferência. Saldo disponível: " + balance);
        }

        if (this.accountNumber.equals(targetAccount.getAccountNumber())) {
            throw new IllegalArgumentException("Não é possível realizar transferência para a mesma conta.");
        }

        // Debita a conta de origem
        this.balance = this.balance.minus(amount);
        register(TransactionType.TRANSFER_OUT, amount, "Transf. para Conta " + targetAccount.getAccountNumber());

        // Credita a conta de destino
        targetAccount.receiveTransfer(this, amount);
    }

    private void receiveTransfer(Account sourceAccount, Money amount) {
        this.balance = this.balance.plus(amount);
        register(TransactionType.TRANSFER_IN, amount, "Transf. de Conta " + sourceAccount.getAccountNumber());
    }

    // ------------------------------------------------------------------
    // Reconstituicao / seed
    // ------------------------------------------------------------------

    /**
     * Anexa ao agregado uma transacao ja existente (carga inicial ou linha
     * vinda do banco), sem disparar regras de negocio nem mexer no saldo.
     */
    public void attachTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction, "A transacao anexada nao pode ser nula");
        this.transactions.add(transaction);
    }

    // ------------------------------------------------------------------
    // Apoio interno
    // ------------------------------------------------------------------

    private void register(TransactionType type, Money amount, String description) {
        transactions.add(new Transaction(UUID.randomUUID(), getId(), type, amount, description));
        touch();
    }

    private void ensureOperational(String message) {
        if (isBlocked()) {
            throw new AccountBlockedException(message);
        }
    }

    private void ensurePositive(Money amount, String message) {
        Objects.requireNonNull(amount, message);
        if (amount.isLessThan(MINIMUM_OPERATION_AMOUNT)) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Zera o consumo do limite diario quando vira o dia. */
    private void resetDailyCounterIfNewDay() {
        LocalDate today = LocalDate.now();
        if (lastWithdrawalDate == null || lastWithdrawalDate.isBefore(today)) {
            this.totalWithdrawnToday = Money.ZERO;
            this.lastWithdrawalDate = today;
        }
    }

    @Override
    public String toString() {
        BigDecimal currentBalance = balance.getAmount();
        return String.format("Account{agency=%s, number=%s, balance=%s, status=%s}",
                agency, accountNumber, currentBalance, status);
    }
}

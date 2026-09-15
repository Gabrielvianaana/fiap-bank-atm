# FIAP Bank ATM — Refatoração DDD (Checkpoint 4)

![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Maven Multi-Module](https://img.shields.io/badge/Maven-Multi--Módulo-blue?style=for-the-badge&logo=apachemaven)
![JDBC](https://img.shields.io/badge/Persistência-JDBC_Puro-green?style=for-the-badge)
![SQLite](https://img.shields.io/badge/Banco-SQLite_3.45-003B57?style=for-the-badge&logo=sqlite)
![DDD](https://img.shields.io/badge/Arquitetura-Domain--Driven_Design-purple?style=for-the-badge)

> **FIAP — Engenharia de Software (2026)**
> **Disciplina:** Domain Driven Design — Java · **Turma:** 2ESPG
> **Professor:** Eduardo dos Santos Ramos
> **Checkpoint 4:** Refactoring do Emulador FIAP Bank ATM

---

## 👥 Integrantes do Grupo

| RM | Nome Completo | GitHub |
| :--- | :--- | :--- |
| **564382** | Gabriel Viana de Souza | [@Gabrielvianaana](https://github.com/Gabrielvianaana) |
| **561714** | Rafael Falaguasta Ferraz | [@Rafael Falaguasta]-(https://github.com/Rafael-Falaguasta) |

**Repositório (fork do projeto base):** `https://github.com/Gabrielvianaana/fiap-bank-atm`
**Repositório original:** https://github.com/prof-eduardo-ramos/fiap-bank-atm

---

## 🎯 O que foi entregue

O emulador legado operava como um **monólito**: todas as classes no mesmo escopo de compilação, a tela Swing acessando diretamente as entidades de domínio, persistência volátil em `HashMap` (perdida a cada reinício), retornos `null` espalhados e laços `for` imperativos.

Esta entrega substitui **exclusivamente o motor da aplicação**, mantendo o *frontend* Swing visualmente e funcionalmente intacto. O usuário final não percebe nenhuma mudança estética — apenas o funcionamento da nova infraestrutura por trás da tela.

| Problema apontado na auditoria | Solução aplicada |
| :--- | :--- |
| Falta de segregação física entre camadas | 4 submódulos Maven independentes, blindados em tempo de compilação |
| Apresentação acessando o domínio | DTOs em **Java Records**; o POM do `presentation` não enxerga `domain` nem `infrastructure` |
| Persistência volátil em memória RAM | Banco relacional **SQLite** via **JDBC puro**, sem ORM |
| Retornos nulos e código imperativo | `Optional<T>` em todas as buscas + **Streams API** nas iterações |
| Risco de SQL Injection | `PreparedStatement` com injeção de parâmetros via métodos `set` |

---

## 🏗️ Arquitetura em Quatro Módulos Físicos

```
fiap-bank-atm/                  ← POM AGREGADOR (packaging: pom)
│
├── domain/                     ← Núcleo soberano de negócio
│   └── com.fiap.bank.atm.domain
│       ├── model/              BaseEntity, Account, Transaction, Money,
│       │                       TransactionType, AccountStatus
│       ├── repository/         ATMRepository<T extends BaseEntity>, AccountRepository
│       └── exception/          AccountBlocked, InvalidPin, InsufficientFunds, DailyLimitExceeded
│
├── application/                ← Casos de uso e contratos de transferência
│   └── com.fiap.bank.atm.application
│       ├── dto/                AccountInfoDTO, TransactionDTO, OperationResultDTO  (Java Records)
│       ├── service/            AtmService
│       ├── mapper/             AtmMapper
│       └── exception/          Contratos de falha publicados para a tela
│
├── infrastructure/             ← Adapters técnicos + Composition Root
│   └── com.fiap.bank.atm
│       ├── AtmApplication      (método main — montagem das camadas)
│       └── infrastructure.persistence/
│           ├── ConnectionFactory           Fábrica de conexões JDBC
│           ├── DatabaseInitializer         Executa o DDL e a carga inicial
│           ├── AccountRepositoryJdbcImpl   Implementação concreta com PreparedStatement
│           └── DataAccessException
│       └── resources/db/       schema.sql, data.sql
│
└── presentation/               ← Interface gráfica Java Swing (INTOCADA)
    └── com.fiap.bank.atm.presentation
        ├── AtmFrame.java / AtmFrame.form
        └── ScreenState.java
```

### Grafo de dependências (blindagem em tempo de compilação)

```
presentation ──────► application ──────► domain
                                            ▲
infrastructure ─────────────────────────────┘
      │ (Composition Root)
      └──► application, presentation
```

**A regra crítica:** o `pom.xml` de `presentation` declara **uma única** dependência de módulo — `application`. Não existe, e o compilador não permitiria, qualquer referência a `domain` ou `infrastructure` a partir do Swing.

> **Por que o `main` mora em `infrastructure`?**
> A regra inviolável nº 2 proíbe o POM da apresentação de apontar para `infrastructure`. Como o *wiring* das camadas precisa conhecer a implementação concreta do repositório, ele foi colocado no módulo mais externo do sistema. Assim a tela permanece 100% cega a JDBC, SQLite e qualquer detalhe de persistência.

---

## ✅ Conformidade com as Regras Invioláveis

### 1. Frontend Intocável (Java Swing)
Nenhuma tela, botão, cor, animação ou transição de estado foi alterada. As **únicas** mudanças em `AtmFrame.java` são os pontos de contato com o motor:

| Antes (domínio exposto) | Depois (contrato de aplicação) |
| :--- | :--- |
| `import ...domain.model.Account` | `import ...application.dto.AccountInfoDTO` |
| `import ...domain.model.Transaction` | `import ...application.dto.TransactionDTO` |
| `import ...domain.exception.*` | `import ...application.exception.*` |
| `acc.getAccountNumber()` | `acc.accountNumber()` |
| `acc.getBalance().format()` | `acc.formattedBalance()` |
| `for (int i = txs.size()-1; ...)` | `atmService.getReceiptStatement()` + `forEach` |

O arquivo `AtmFrame.form` e todo o método de construção de layout permanecem byte a byte iguais ao original.

### 2. Isolamento Físico Estrito (Apache Maven)
Quatro submódulos obrigatórios criados, classes migradas fisicamente para os respectivos `src/main/java` e dependências editadas com rigor. Verificação prática: compilar `presentation` com um classpath contendo **apenas** `application` funciona — prova de que nenhuma classe de domínio é alcançável.

### 3. Proteção contra SQL Injection
Zero concatenação de Strings para montagem de SQL. Todos os comandos são constantes com marcadores `?`, alimentados exclusivamente por `setString`, `setBigDecimal` e `setInt`. A classe `Statement` básica aparece apenas em dois pontos sem dado de usuário: o `PRAGMA foreign_keys` e a execução do script DDL de criação de tabelas.

### 4. Proibição de ORMs
Nenhum Hibernate, JPA ou Spring Data. As únicas dependências externas do projeto são `org.xerial:sqlite-jdbc` (driver oficial) e `com.formdev:flatlaf` (Look & Feel, já presente no original).

### 5. Uso Defensivo e Erradicação do Null
A interface `ATMRepository<T extends BaseEntity>` obriga `Optional<T>` em toda busca. A camada orquestradora trata a ausência ativamente com `orElseThrow`, `map` e `flatMap` — nunca com comparações contra `null`. Até a sessão ativa do terminal é um `Optional<Account>`, não um campo anulável.

### 6. Versionamento e Colaboração
Histórico de commits distribuído entre os integrantes no repositório do fork.

---

## 🔑 Peças-Chave da Refatoração

### Repositório Genérico com Limite Superior

```java
public interface ATMRepository<T extends BaseEntity> {
    Optional<T> buscarPorId(UUID id);
    void salvar(T entidade);
    void remover(UUID id);
    List<T> buscarTodos();
}

public interface AccountRepository extends ATMRepository<Account> {
    Optional<Account> findByAccountNumber(String accountNumber);
}
```

### Contratos de Transferência em Java Records

```java
public record AccountInfoDTO(
        UUID id, String agency, String accountNumber,
        BigDecimal balance, String formattedBalance,
        BigDecimal dailyWithdrawalLimit, BigDecimal totalWithdrawnToday,
        BigDecimal remainingDailyLimit, String formattedRemainingDailyLimit,
        String status, Boolean blocked, Integer failedAttempts) { }
```

Apenas `UUID`, `BigDecimal`, `String` e wrappers (`Boolean`, `Integer`) atravessam a fronteira. Nenhum `Account`, `Money` ou `Transaction` escapa do domínio.

### Consulta Parametrizada com PreparedStatement e ResultSet

```java
private static final String SELECT_BY_NUMBER = SELECT_ACCOUNT_COLUMNS + " WHERE number = ?";

try (Connection connection = connectionFactory.getConnection();
     PreparedStatement statement = connection.prepareStatement(SELECT_BY_NUMBER)) {

    statement.setString(1, accountNumber);      // sanitização garantida

    try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
            return Optional.empty();            // ausência ≠ null
        }
        Account account = mapAccount(resultSet);
        loadTransactions(account);
        return Optional.of(account);
    }
}
```

### Streams API no lugar dos laços imperativos

```java
// Extrato: ordenação + corte, sem índices manuais
public List<Transaction> getLatestTransactions(int limit) {
    return transactions.stream()
            .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
            .limit(Math.max(limit, 0))
            .toList();
}

// Somatório por tipo, sem acumulador em laço
public Money sumByType(TransactionType type) {
    return transactions.stream()
            .filter(transaction -> transaction.getType() == type)
            .map(Transaction::getAmount)
            .reduce(Money.ZERO, Money::plus);
}
```

---

## 🗄️ Modelo de Dados

Baseado no dicionário do **Anexo 7.2**. As colunas marcadas como *extensão* foram acrescentadas ao final de cada tabela — todas com `DEFAULT` — para persistir o estado de negócio que o emulador já mantinha em memória. Sem elas, o frontend intocado deixaria de funcionar. A carga do Anexo 7.2 roda sem qualquer alteração.

### `tb_account`

| Coluna | Tipo | Origem |
| :--- | :--- | :--- |
| `id` | VARCHAR(36) PK | Anexo 7.2 |
| `agency` | VARCHAR(10) NOT NULL | Anexo 7.2 |
| `number` | VARCHAR(20) NOT NULL | Anexo 7.2 |
| `balance` | DECIMAL(15,2) NOT NULL | Anexo 7.2 |
| `status` | VARCHAR(20) NOT NULL | Anexo 7.2 |
| `pin` | VARCHAR(4) DEFAULT '0000' | extensão |
| `daily_withdrawal_limit` | DECIMAL(15,2) DEFAULT 1000.00 | extensão |
| `total_withdrawn_today` | DECIMAL(15,2) DEFAULT 0.00 | extensão |
| `last_withdrawal_date` | VARCHAR(10) | extensão |
| `failed_attempts` | INTEGER DEFAULT 0 | extensão |
| `created_at` / `updated_at` | TIMESTAMP | extensão (auditoria) |

### `tb_transaction`

| Coluna | Tipo | Origem |
| :--- | :--- | :--- |
| `id` | VARCHAR(36) PK | Anexo 7.2 |
| `account_id` | VARCHAR(36) FK → `tb_account(id)` | Anexo 7.2 |
| `type` | VARCHAR(20) NOT NULL | Anexo 7.2 |
| `amount` | DECIMAL(15,2) NOT NULL | Anexo 7.2 |
| `created_at` | TIMESTAMP NOT NULL | Anexo 7.2 |
| `description` | VARCHAR(120) DEFAULT '' | extensão (histórico do extrato) |

Os scripts `schema.sql` e `data.sql` (em `infrastructure/src/main/resources/db/`) são **idempotentes** (`CREATE TABLE IF NOT EXISTS` / `ON CONFLICT DO NOTHING`), então reiniciar a aplicação preserva integralmente os dados já gravados.

---

## 🔑 Contas para Teste

Contas operacionais do terminal — as mesmas credenciais do emulador original, agora gravadas no banco relacional:

| Conta | PIN | Saldo Inicial | Limite Diário |
| :---: | :---: | :---: | :---: |
| `12345` | `1234` | R$ 5.000,00 | R$ 1.500,00 |
| `67890` | `5678` | R$ 1.200,00 | R$ 1.000,00 |
| `99999` | `9999` | R$ 50,00 | R$ 500,00 |

A carga também inclui as três contas literais do Anexo 7.2 (`12345-6`, `98765-4`, `11111-1`), utilizadas como referência do dicionário de dados.

> Após o primeiro uso, os saldos passam a refletir as operações realizadas — a persistência é real e sobrevive ao reinício. Para voltar ao estado inicial, apague o arquivo `fiap-bank-atm.db` da raiz do projeto.

---

## 🚀 Como Executar

### Pré-requisitos
- **JDK 21** ou superior (`JAVA_HOME` configurado)
- **Apache Maven 3.8+**

### Linha de comando

```bash
# 1) Compila e instala os quatro módulos no repositório local
mvn clean install

# 2) Executa a aplicação a partir do Composition Root
mvn -pl infrastructure exec:java
```

### Scripts prontos

```bash
./run.sh      # macOS / Linux
```
```cmd
run.bat       :: Windows (localiza o Maven do NetBeans ou o global)
```

### IDE (NetBeans / IntelliJ / Eclipse / VS Code)

1. Abra o projeto pelo `pom.xml` **da raiz** — a IDE reconhece os quatro submódulos automaticamente.
2. Rode `mvn clean install` uma vez para publicar os módulos no repositório local.
3. Execute a classe `com.fiap.bank.atm.AtmApplication` (módulo `infrastructure`).

### Testes

```bash
mvn test
```

O módulo `domain` traz uma suíte JUnit 5 cobrindo saque, depósito, transferência, bloqueio por tentativas, limite diário, somatórios com Streams e o contrato `Optional`.

---

## 📦 Dependências

| Biblioteca | Versão | Módulo | Finalidade |
| :--- | :--- | :--- | :--- |
| `org.xerial:sqlite-jdbc` | 3.45.1.0 | `infrastructure` | Driver JDBC oficial do SQLite (Anexo 7.1) |
| `com.formdev:flatlaf` | 3.5.1 | `presentation` | Look & Feel escuro da UI Swing |
| `org.junit.jupiter` | 5.10.2 | testes | Suíte de testes unitários |

O módulo `domain` não possui **nenhuma** dependência externa — apenas a API nativa do JDK 21.

---

## 📝 Créditos

Trabalho acadêmico desenvolvido para a disciplina de **Domain Driven Design — Java**, curso de **Engenharia de Software** da **FIAP** (2026), turma **2ESPG**, sob orientação do **Prof. Eduardo dos Santos Ramos**.

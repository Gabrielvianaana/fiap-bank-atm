-- =====================================================================
-- FIAP BANK ATM - Checkpoint 4
-- Dicionario de Dados (DDL) - base do Anexo 7.2 do enunciado.
--
-- As cinco primeiras colunas de tb_account e todas as colunas de
-- tb_transaction reproduzem literalmente o Anexo 7.2. As colunas
-- acrescentadas ao final de cada tabela (marcadas como EXTENSAO) sao as
-- necessarias para persistir o estado de negocio que o emulador ja
-- possuia em memoria - credencial, limite diario e auditoria - sem as
-- quais o frontend Swing intocado deixaria de funcionar.
-- Todas possuem DEFAULT, de modo que a carga do Anexo 7.2 roda sem
-- qualquer alteracao.
-- =====================================================================

-- Criacao das Tabelas do Sistema (DDL)
CREATE TABLE IF NOT EXISTS tb_account (
    id VARCHAR(36) PRIMARY KEY,
    agency VARCHAR(10) NOT NULL,
    number VARCHAR(20) NOT NULL,
    balance DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    -- EXTENSAO: estado de negocio do terminal
    pin VARCHAR(4) NOT NULL DEFAULT '0000',
    daily_withdrawal_limit DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
    total_withdrawn_today DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    last_withdrawal_date VARCHAR(10),
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime')),
    updated_at TIMESTAMP NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS tb_transaction (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    -- EXTENSAO: historico descritivo exibido no extrato impresso
    description VARCHAR(120) NOT NULL DEFAULT '',
    FOREIGN KEY (account_id) REFERENCES tb_account(id)
);

-- Indices de apoio as consultas do terminal
CREATE UNIQUE INDEX IF NOT EXISTS ux_account_number ON tb_account (number);
CREATE INDEX IF NOT EXISTS ix_transaction_account ON tb_transaction (account_id, created_at DESC);

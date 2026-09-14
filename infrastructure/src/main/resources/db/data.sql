-- =====================================================================
-- FIAP BANK ATM - Checkpoint 4
-- Carga Inicial (DML) - idempotente via ON CONFLICT DO NOTHING.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOCO 1 - Carga Inicial de Contas de Teste do Anexo 7.2 (literal)
-- ---------------------------------------------------------------------
INSERT INTO tb_account (id, agency, number, balance, status)
VALUES ('550e8400-e29b-41d4-a716-446655440000', '0001', '12345-6', 1500.00, 'ACTIVE')
ON CONFLICT DO NOTHING;

INSERT INTO tb_account (id, agency, number, balance, status)
VALUES ('550e8400-e29b-41d4-a716-446655440001', '0001', '98765-4', 250.50, 'ACTIVE')
ON CONFLICT DO NOTHING;

INSERT INTO tb_account (id, agency, number, balance, status)
VALUES ('550e8400-e29b-41d4-a716-446655440002', '0002', '11111-1', 0.00, 'BLOCKED')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- BLOCO 2 - Contas operacionais do terminal
-- Reproduzem exatamente o seed que antes vivia em memoria no
-- InMemoryAccountRepository, garantindo que o frontend Swing intocado
-- continue autenticando com as mesmas credenciais de sempre.
-- ---------------------------------------------------------------------
INSERT INTO tb_account (id, agency, number, balance, status, pin, daily_withdrawal_limit, total_withdrawn_today, last_withdrawal_date, failed_attempts)
VALUES ('7f000101-0000-4000-a000-000000012345', '0001', '12345', 5000.00, 'ACTIVE', '1234', 1500.00, 0.00, strftime('%Y-%m-%d', 'now', 'localtime'), 0)
ON CONFLICT DO NOTHING;

INSERT INTO tb_account (id, agency, number, balance, status, pin, daily_withdrawal_limit, total_withdrawn_today, last_withdrawal_date, failed_attempts)
VALUES ('7f000101-0000-4000-a000-000000067890', '0001', '67890', 1200.00, 'ACTIVE', '5678', 1000.00, 0.00, strftime('%Y-%m-%d', 'now', 'localtime'), 0)
ON CONFLICT DO NOTHING;

INSERT INTO tb_account (id, agency, number, balance, status, pin, daily_withdrawal_limit, total_withdrawn_today, last_withdrawal_date, failed_attempts)
VALUES ('7f000101-0000-4000-a000-000000099999', '0002', '99999', 50.00, 'ACTIVE', '9999', 500.00, 0.00, strftime('%Y-%m-%d', 'now', 'localtime'), 0)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- BLOCO 3 - Historico inicial de movimentacoes (extrato de demonstracao)
-- ---------------------------------------------------------------------
INSERT INTO tb_transaction (id, account_id, type, amount, created_at, description)
VALUES ('7f000102-0000-4000-a000-000000000001', '7f000101-0000-4000-a000-000000012345', 'DEPOSIT', 2000.00, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-3 days'), 'Depósito em dinheiro')
ON CONFLICT DO NOTHING;

INSERT INTO tb_transaction (id, account_id, type, amount, created_at, description)
VALUES ('7f000102-0000-4000-a000-000000000002', '7f000101-0000-4000-a000-000000012345', 'TRANSFER_IN', 500.00, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-2 days'), 'Transf. de Conta 67890')
ON CONFLICT DO NOTHING;

INSERT INTO tb_transaction (id, account_id, type, amount, created_at, description)
VALUES ('7f000102-0000-4000-a000-000000000003', '7f000101-0000-4000-a000-000000012345', 'WITHDRAWAL', 100.00, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-1 days'), 'Saque eletrônico')
ON CONFLICT DO NOTHING;

INSERT INTO tb_transaction (id, account_id, type, amount, created_at, description)
VALUES ('7f000102-0000-4000-a000-000000000004', '7f000101-0000-4000-a000-000000067890', 'DEPOSIT', 1500.00, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-5 days'), 'Depósito inicial')
ON CONFLICT DO NOTHING;

INSERT INTO tb_transaction (id, account_id, type, amount, created_at, description)
VALUES ('7f000102-0000-4000-a000-000000000005', '7f000101-0000-4000-a000-000000067890', 'TRANSFER_OUT', 500.00, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-2 days'), 'Transf. para Conta 12345')
ON CONFLICT DO NOTHING;

INSERT INTO tb_transaction (id, account_id, type, amount, created_at, description)
VALUES ('7f000102-0000-4000-a000-000000000006', '7f000101-0000-4000-a000-000000099999', 'DEPOSIT', 50.00, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-10 days'), 'Abertura de conta')
ON CONFLICT DO NOTHING;

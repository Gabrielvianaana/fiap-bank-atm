#!/usr/bin/env bash
# ====================================================
#        FIAP BANK - EMULADOR DE CAIXA ELETRONICO
#        Checkpoint 4 - Refatoracao DDD (multi-modulo)
# ====================================================
set -e

echo "===================================================="
echo "   FIAP BANK ATM - Checkpoint 4 (macOS / Linux)"
echo "===================================================="

if ! command -v mvn >/dev/null 2>&1; then
    echo "[ERRO] Maven nao encontrado no PATH."
    echo "       Instale com 'brew install maven' ou abra o projeto na sua IDE."
    exit 1
fi

echo "==> Compilando os quatro modulos (domain, application, infrastructure, presentation)..."
mvn clean install

echo "==> Iniciando a aplicacao a partir do Composition Root..."
mvn -pl infrastructure exec:java

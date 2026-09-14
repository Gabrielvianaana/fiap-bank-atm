package com.fiap.bank.atm.application.dto;

import java.math.BigDecimal;

/**
 * Contrato de transferencia (DTO) devolvido apos uma operacao financeira
 * (saque, deposito ou transferencia), permitindo que a tela confirme o
 * resultado sem nunca tocar no agregado de dominio.
 *
 * <p>
 * Implementado como <b>Java Record</b>: imutavel e sem estado compartilhado.
 * </p>
 *
 * @param success            indicador de sucesso da operacao
 * @param operation          nome tecnico da operacao executada
 * @param amount             valor movimentado
 * @param formattedAmount    valor movimentado ja formatado em pt-BR
 * @param resultingBalance   saldo resultante apos a operacao
 * @param formattedBalance   saldo resultante ja formatado em pt-BR
 * @param message            mensagem descritiva do desfecho
 */
public record OperationResultDTO(
        Boolean success,
        String operation,
        BigDecimal amount,
        String formattedAmount,
        BigDecimal resultingBalance,
        String formattedBalance,
        String message) {
}

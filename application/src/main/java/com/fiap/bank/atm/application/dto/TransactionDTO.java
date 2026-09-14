package com.fiap.bank.atm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contrato de transferencia (DTO) de uma movimentacao do extrato.
 *
 * <p>
 * Implementado como <b>Java Record</b>, garantindo leveza e imutabilidade. A
 * entidade {@code Transaction} do dominio permanece encapsulada e invisivel
 * para a camada de apresentacao.
 * </p>
 *
 * @param id                  identificador unico da movimentacao
 * @param accountId           conta a qual a movimentacao pertence
 * @param type                tipo tecnico (WITHDRAWAL, DEPOSIT, TRANSFER_OUT, TRANSFER_IN)
 * @param typeDescription     tipo em linguagem de negocio (ex.: {@code Saque})
 * @param amount              valor bruto da movimentacao
 * @param formattedAmount     valor ja formatado em pt-BR
 * @param createdAt           data e hora do lancamento
 * @param formattedTimestamp  data e hora ja formatadas
 * @param description         historico descritivo do lancamento
 */
public record TransactionDTO(
        UUID id,
        UUID accountId,
        String type,
        String typeDescription,
        BigDecimal amount,
        String formattedAmount,
        LocalDateTime createdAt,
        String formattedTimestamp,
        String description) {
}

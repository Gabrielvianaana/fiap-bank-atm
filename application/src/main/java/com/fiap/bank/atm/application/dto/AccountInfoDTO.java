package com.fiap.bank.atm.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Contrato de transferencia (DTO) que representa a conta autenticada para a
 * camada de apresentacao.
 *
 * <p>
 * Implementado como <b>Java Record</b>: leve, imutavel por construcao e com
 * {@code equals}, {@code hashCode} e {@code toString} gerados pelo compilador.
 * </p>
 *
 * <p>
 * <b>Blindagem:</b> nenhum campo aqui e uma entidade de dominio. Trafegam
 * apenas {@link UUID}, {@link BigDecimal}, {@link String} e wrappers
 * ({@link Boolean}, {@link Integer}). A tela Swing jamais toca em
 * {@code Account}, {@code Money} ou {@code Transaction}.
 * </p>
 *
 * <p>
 * Os campos {@code formatted*} carregam o valor ja formatado em pt-BR,
 * mantendo a regra de apresentacao monetaria dentro do dominio e deixando a
 * tela como mera renderizadora de texto.
 * </p>
 *
 * @param id                            identificador unico da conta
 * @param agency                        agencia da conta
 * @param accountNumber                 numero da conta exibido no terminal
 * @param balance                       saldo disponivel em valor bruto
 * @param formattedBalance              saldo ja formatado (ex.: {@code R$ 5.000,00})
 * @param dailyWithdrawalLimit          limite diario de saque contratado
 * @param totalWithdrawnToday           total ja sacado no dia corrente
 * @param remainingDailyLimit           limite diario ainda disponivel
 * @param formattedRemainingDailyLimit  limite restante ja formatado
 * @param status                        situacao cadastral (ACTIVE / BLOCKED)
 * @param blocked                       indicador de bloqueio
 * @param failedAttempts                tentativas de senha malsucedidas
 */
public record AccountInfoDTO(
        UUID id,
        String agency,
        String accountNumber,
        BigDecimal balance,
        String formattedBalance,
        BigDecimal dailyWithdrawalLimit,
        BigDecimal totalWithdrawnToday,
        BigDecimal remainingDailyLimit,
        String formattedRemainingDailyLimit,
        String status,
        Boolean blocked,
        Integer failedAttempts) {
}

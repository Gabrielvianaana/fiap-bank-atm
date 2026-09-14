package com.fiap.bank.atm.domain.repository;

import com.fiap.bank.atm.domain.model.Account;

import java.util.Optional;

/**
 * Contrato especializado da raiz de agregado {@link Account}.
 *
 * <p>
 * Estende a abstracao generica {@link ATMRepository}, herdando
 * {@code buscarPorId}, {@code salvar}, {@code remover} e {@code buscarTodos},
 * e acrescenta apenas a consulta propria do negocio bancario.
 * </p>
 */
public interface AccountRepository extends ATMRepository<Account> {

    /**
     * Localiza uma conta pelo numero informado no terminal.
     *
     * @return {@link Optional} com a conta quando existente; {@link Optional#empty()}
     *         quando nao houver correspondencia. Jamais {@code null}.
     */
    Optional<Account> findByAccountNumber(String accountNumber);
}

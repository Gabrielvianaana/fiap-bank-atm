package com.fiap.bank.atm.application.exception;

/**
 * Raiz da hierarquia de falhas de negocio publicadas pela camada de aplicacao.
 *
 * <p>
 * <b>Por que existir?</b> A camada de apresentacao nao pode enxergar o pacote
 * {@code domain} (regra inviolavel nº 2). As excecoes de negocio nascem no
 * dominio, mas precisam chegar a tela para orientar o usuario. O servico de
 * aplicacao atua como tradutor: captura a excecao de dominio e a republica
 * neste contrato, que o Swing conhece legitimamente.
 * </p>
 */
public class AtmOperationException extends RuntimeException {

    public AtmOperationException(String message) {
        super(message);
    }

    public AtmOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

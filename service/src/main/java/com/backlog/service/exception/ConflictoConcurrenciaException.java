package com.backlog.service.exception;

/**
 * Se lanza cuando un elemento fue modificado por otra transacción desde que el
 * usuario leyó sus datos, para evitar que un cambio sobrescriba otro.
 */
public class ConflictoConcurrenciaException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje que se mostrará al usuario.
     *
     * @param message mensaje descriptivo del conflicto de concurrencia
     */
    public ConflictoConcurrenciaException(String message) {
        super(message);
    }
}

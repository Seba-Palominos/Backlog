package com.backlog.service.exception;

/**
 * Se lanza cuando el usuario autenticado no tiene el permiso necesario
 * sobre un proyecto para realizar una acción sobre sus elementos.
 */
public class PermisoDenegadoException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje que se mostrará al usuario.
     *
     * @param message mensaje descriptivo del permiso faltante
     */
    public PermisoDenegadoException(String message) {
        super(message);
    }
}

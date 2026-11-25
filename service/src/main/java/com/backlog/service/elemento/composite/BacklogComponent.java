package com.backlog.service.elemento.composite;

/**
 * Componente del patrón Composite para las operaciones sobre el backlog.
 * El mensaje {@code delete} se propaga por igual a hojas y compuestos.
 */
public interface BacklogComponent {

    /**
     * Elimina el componente, propagando la operación a sus elementos hijos.
     */
    void delete();
}

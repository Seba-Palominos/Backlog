package com.backlog.service.elemento.composite;

import com.backlog.service.elemento.entitys.Elemento;

import java.util.function.Consumer;

/**
 * Hoja del Composite: un elemento del backlog sin hijos.
 */
public class HojaElemento implements BacklogComponent {

    private final Elemento elemento;
    private final Consumer<Elemento> deleteAction;

    /**
     * Crea la hoja con el elemento y la acción que debe ejecutar al borrarse.
     *
     * @param elemento elemento persistido
     * @param deleteAction acción de borrado de un elemento
     */
    public HojaElemento(Elemento elemento, Consumer<Elemento> deleteAction) {
        this.elemento = elemento;
        this.deleteAction = deleteAction;
    }

    @Override
    public void delete() {
        deleteAction.accept(elemento);
    }
}

package com.backlog.service.elemento.composite;

import com.backlog.service.elemento.entitys.Elemento;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Compuesto del Composite: un directorio (o la raíz del repositorio) con elementos hijos.
 * Al borrarse envía el mismo mensaje a cada hijo antes de eliminarse a sí mismo.
 */
public class DirectorioComposite implements BacklogComponent {

    private final Elemento directorio;
    private final Consumer<Elemento> deleteAction;
    private final List<BacklogComponent> children = new ArrayList<>();

    /**
     * Crea el compuesto. Si el directorio es {@code null} representa la raíz del proyecto.
     *
     * @param directorio elemento directorio o {@code null} para la raíz
     * @param deleteAction acción de borrado de un elemento
     */
    public DirectorioComposite(Elemento directorio, Consumer<Elemento> deleteAction) {
        this.directorio = directorio;
        this.deleteAction = deleteAction;
    }

    /**
     * Agrega un hijo al compuesto.
     *
     * @param child componente hijo
     */
    public void add(BacklogComponent child) {
        children.add(child);
    }

    @Override
    public void delete() {
        children.forEach(BacklogComponent::delete);
        if (directorio != null) {
            deleteAction.accept(directorio);
        }
    }
}

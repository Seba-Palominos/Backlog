package com.backlog.service.autenticacion.entitys;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Acciones que un usuario puede realizar sobre los elementos de un proyecto.
 * Los alias se comparan con el campo tipo de la entidad Permiso.
 */
public enum PermisoAccion {

    LEER("leer", "lectura", "solo lectura", "read"),
    CREAR("crear", "creación", "creacion", "create"),
    MODIFICAR("modificar", "modificación", "modificacion", "update", "editar"),
    ELIMINAR("eliminar", "borrar", "eliminación", "eliminacion", "delete");

    private static final Set<String> PERMISO_TODOS =
            Set.of("todos", "todas", "all", "admin", "administrador", "todos admin", "todos (admin)");

    private final Set<String> alias;

    PermisoAccion(String... alias) {
        this.alias = Set.of(alias);
    }

    /**
     * Traduce el tipo almacenado en Permiso a las acciones que habilita.
     *
     * @param tipo valor del campo tipo
     * @return acciones permitidas por ese tipo de permiso
     */
    public static Set<PermisoAccion> fromTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return EnumSet.noneOf(PermisoAccion.class);
        }

        String normalizado = normalize(tipo);
        if (PERMISO_TODOS.contains(normalizado)) {
            return EnumSet.allOf(PermisoAccion.class);
        }

        EnumSet<PermisoAccion> acciones = EnumSet.noneOf(PermisoAccion.class);
        for (PermisoAccion accion : values()) {
            if (accion.alias.contains(normalizado)) {
                acciones.add(accion);
            }
        }
        return acciones;
    }

    /**
     * Quita acentos y espacios sobrantes y convierte el texto a minúsculas.
     *
     * @param valor texto a normalizar
     * @return texto normalizado
     */
    private static String normalize(String valor) {
        return Normalizer.normalize(valor.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}

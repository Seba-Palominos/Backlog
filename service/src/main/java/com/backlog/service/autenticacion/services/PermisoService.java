package com.backlog.service.autenticacion.services;

import com.backlog.service.autenticacion.entitys.Permiso;
import com.backlog.service.autenticacion.entitys.PermisoAccion;
import com.backlog.service.autenticacion.entitys.TienePermiso;
import com.backlog.service.autenticacion.repository.TienePermisoRepository;
import com.backlog.service.exception.PermisoDenegadoException;
import com.backlog.service.proyecto.entitys.Proyecto;
import com.backlog.service.proyecto.repository.ProyectoRepository;
import com.backlog.service.registro.entitys.Usuario;
import com.backlog.service.registro.repository.UsuarioRepository;
import com.backlog.service.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio que resuelve los permisos de un usuario sobre un proyecto
 * a partir de la relación TienePermiso del modelo de base de datos.
 */
@Service
public class PermisoService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private TienePermisoRepository tienePermisoRepository;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    /**
     * Comprueba si el usuario autenticado puede realizar una acción sobre los
     * elementos de un proyecto. El propietario siempre tiene todos los permisos.
     *
     * @param projectId identificador del proyecto
     * @param action acción a comprobar
     * @return {@code true} si la acción está permitida
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(Long projectId, PermisoAccion action) {
        if (projectId == null || action == null) {
            return false;
        }

        Usuario user = getCurrentUser();
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("El repositorio no existe"));

        if (proyecto.getOwner().getId().equals(user.getId())) {
            return true;
        }

        return permisosDeUsuario(user, projectId).contains(action);
    }

    /**
     * Comprueba si un usuario tiene algún permiso asignado dentro de un proyecto.
     *
     * @param user usuario a comprobar
     * @param projectId identificador del proyecto
     * @return {@code true} si tiene al menos un permiso válido
     */
    @Transactional(readOnly = true)
    public boolean hasAnyPermission(Usuario user, Long projectId) {
        if (user == null || projectId == null) {
            return false;
        }
        return !permisosDeUsuario(user, projectId).isEmpty();
    }

    /**
     * Exige un permiso o lanza una excepción traducida a HTTP 403.
     *
     * @param projectId identificador del proyecto
     * @param action acción requerida
     */
    @Transactional(readOnly = true)
    public void requirePermission(Long projectId, PermisoAccion action) {
        if (!hasPermission(projectId, action)) {
            throw new PermisoDenegadoException(
                    "No tienes permiso para " + action.name().toLowerCase(Locale.ROOT)
                            + " elementos en este repositorio");
        }
    }

    /**
     * Reúne las acciones habilitadas por los permisos asignados al usuario.
     *
     * @param user usuario consultado
     * @param projectId identificador del proyecto
     * @return conjunto de acciones permitidas
     */
    private Set<PermisoAccion> permisosDeUsuario(Usuario user, Long projectId) {
        return tienePermisoRepository.findByIdUserIdAndIdProjectId(user.getId(), projectId).stream()
                .map(TienePermiso::getPermission)
                .filter(Objects::nonNull)
                .map(Permiso::getType)
                .flatMap(tipo -> PermisoAccion.fromTipo(tipo).stream())
                .collect(Collectors.toSet());
    }

    /**
     * Recupera el usuario autenticado a partir del nombre del token.
     *
     * @return usuario autenticado
     */
    private Usuario getCurrentUser() {
        String username = currentUserProvider.getRequiredUsername();
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("El usuario autenticado no existe"));
    }
}

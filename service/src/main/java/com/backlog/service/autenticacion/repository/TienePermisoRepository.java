package com.backlog.service.autenticacion.repository;

import com.backlog.service.autenticacion.entitys.TienePermiso;
import com.backlog.service.autenticacion.entitys.TienePermisoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad TienePermiso.
 */
public interface TienePermisoRepository extends JpaRepository<TienePermiso, TienePermisoId> {

    // Lista los permisos asignados a un usuario.
    List<TienePermiso> findByIdUserId(Long userId);

    // Lista los permisos asignados dentro de un proyecto.
    List<TienePermiso> findByIdProjectId(Long projectId);

    // Lista los permisos de un usuario dentro de un proyecto.
    List<TienePermiso> findByIdUserIdAndIdProjectId(Long userId, Long projectId);

    // Comprueba si un usuario tiene un permiso concreto en un proyecto.
    boolean existsByIdUserIdAndIdPermissionIdAndIdProjectId(Long userId, Long permissionId, Long projectId);
}

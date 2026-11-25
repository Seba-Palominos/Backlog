package com.backlog.service.autenticacion.repository;

import com.backlog.service.autenticacion.entitys.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Permiso.
 */
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    // Busca un permiso por su tipo.
    Optional<Permiso> findByType(String type);

    // Comprueba si ya existe un permiso con el tipo indicado.
    boolean existsByType(String type);
}

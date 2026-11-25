package com.backlog.service.proyecto.repository;

import com.backlog.service.proyecto.entitys.Participa;
import com.backlog.service.proyecto.entitys.ParticipaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad Participa.
 */
public interface ParticipaRepository extends JpaRepository<Participa, ParticipaId> {

    // Lista las participaciones de un usuario.
    List<Participa> findByIdUserId(Long userId);

    // Lista los participantes de un proyecto.
    List<Participa> findByIdProjectId(Long projectId);

    // Comprueba si un usuario participa en un proyecto.
    boolean existsByIdUserIdAndIdProjectId(Long userId, Long projectId);
}

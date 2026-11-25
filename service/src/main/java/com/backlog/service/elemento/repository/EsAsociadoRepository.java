package com.backlog.service.elemento.repository;

import com.backlog.service.elemento.entitys.EsAsociado;
import com.backlog.service.elemento.entitys.EsAsociadoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad EsAsociado.
 */
public interface EsAsociadoRepository extends JpaRepository<EsAsociado, EsAsociadoId> {

    // Lista las asociaciones de una historia de usuario.
    List<EsAsociado> findByIdStoryId(Long storyId);

    // Lista las historias asociadas a una épica.
    List<EsAsociado> findByIdEpicId(Long epicId);

    // Comprueba si una historia ya está asociada a una épica.
    boolean existsByIdStoryIdAndIdEpicId(Long storyId, Long epicId);

    // Lista las asociaciones de todas las historias de un proyecto.
    @Query("SELECT a FROM EsAsociado a WHERE a.story.project.id = :projectId")
    List<EsAsociado> findByProjectId(@Param("projectId") Long projectId);
}

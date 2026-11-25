package com.backlog.service.elemento.repository;

import com.backlog.service.elemento.entitys.Directorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad Directorio.
 */
public interface DirectorioRepository extends JpaRepository<Directorio, Long> {

    // Lista los directorios de un proyecto.
    List<Directorio> findByProject_Id(Long projectId);

    // Lista los directorios hijos directos de un elemento.
    List<Directorio> findByParent_Id(Long parentId);
}

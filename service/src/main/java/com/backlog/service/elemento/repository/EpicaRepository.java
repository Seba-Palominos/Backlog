package com.backlog.service.elemento.repository;

import com.backlog.service.elemento.entitys.Epica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad Epica.
 */
public interface EpicaRepository extends JpaRepository<Epica, Long> {

    // Lista las épicas de un proyecto.
    List<Epica> findByProject_Id(Long projectId);

    // Cuenta las épicas de un proyecto.
    long countByProject_Id(Long projectId);
}

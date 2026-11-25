package com.backlog.service.proyecto.repository;

import com.backlog.service.proyecto.entitys.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad Proyecto.
 */
public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    // Lista los proyectos cuyo propietario es el usuario indicado.
    List<Proyecto> findByOwner_Id(Long ownerId);
}

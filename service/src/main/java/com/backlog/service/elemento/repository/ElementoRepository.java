package com.backlog.service.elemento.repository;

import com.backlog.service.elemento.entitys.Elemento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad base Elemento.
 */
public interface ElementoRepository extends JpaRepository<Elemento, Long> {

    // Lista los elementos de un proyecto.
    List<Elemento> findByProject_Id(Long projectId);

    // Lista los elementos raíz (sin padre) de un proyecto.
    List<Elemento> findByProject_IdAndParentIsNull(Long projectId);

    // Lista los hijos directos de un elemento.
    List<Elemento> findByParent_Id(Long parentId);

    // Busca elementos de un proyecto cuyo título contenga el texto indicado.
    List<Elemento> findByProject_IdAndTitleContainingIgnoreCase(Long projectId, String title);
}

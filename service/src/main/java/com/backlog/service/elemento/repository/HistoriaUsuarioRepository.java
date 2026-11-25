package com.backlog.service.elemento.repository;

import com.backlog.service.elemento.entitys.HistoriaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad HistoriaUsuario.
 */
public interface HistoriaUsuarioRepository extends JpaRepository<HistoriaUsuario, Long> {

    // Busca una historia de usuario por su identificador funcional.
    Optional<HistoriaUsuario> findByIdentifier(String identifier);

    // Lista las historias de usuario de un proyecto.
    List<HistoriaUsuario> findByProject_Id(Long projectId);

    // Cuenta las historias de usuario de un proyecto.
    long countByProject_Id(Long projectId);
}

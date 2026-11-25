package com.backlog.service.registro.repository;

import com.backlog.service.registro.entitys.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Usuario.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca un usuario por su nombre de usuario.
    Optional<Usuario> findByUsername(String username);

    // Busca un usuario por su correo sin distinguir mayúsculas y minúsculas.
    Optional<Usuario> findByEmailIgnoreCase(String email);

    // Comprueba si ya existe un usuario con el nombre indicado.
    boolean existsByUsername(String username);

    // Comprueba si ya existe un usuario con el correo indicado, sin distinguir mayúsculas.
    boolean existsByEmailIgnoreCase(String email);
}

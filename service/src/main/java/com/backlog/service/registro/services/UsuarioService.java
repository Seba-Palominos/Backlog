package com.backlog.service.registro.services;

import com.backlog.service.registro.dto.RegistroRequest;
import com.backlog.service.registro.entitys.Usuario;
import com.backlog.service.registro.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio con la lógica de negocio del registro de usuarios.
 */
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo usuario a partir de los datos del formulario.
     *
     * @param request datos del formulario de registro
     * @return el usuario persistido
     * @throws IllegalArgumentException si los datos no son válidos o ya existen
     */
    public Usuario register(RegistroRequest request) {
        validateRequest(request);

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        // No se permite duplicar el nombre de usuario ni el correo.
        if (usuarioRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(email);
        // La contraseña se almacena siempre como hash, nunca en texto plano.
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));

        return usuarioRepository.save(usuario);
    }

    /**
     * Valida de forma explícita los datos recibidos del formulario.
     *
     * @param request datos del formulario de registro
     * @throws IllegalArgumentException si algún dato es inválido
     */
    private void validateRequest(RegistroRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del registro son obligatorios");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        if (request.getConfirmPassword() == null
                || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
    }
}

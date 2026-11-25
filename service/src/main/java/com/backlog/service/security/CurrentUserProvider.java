package com.backlog.service.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Proveedor que expone el usuario autenticado en la petición actual,
 * a partir del contexto de seguridad de Spring.
 */
@Component
public class CurrentUserProvider {

    /**
     * Obtiene el nombre del usuario autenticado, si existe.
     *
     * @return un {@link Optional} con el nombre de usuario
     */
    public Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        return Optional.ofNullable(authentication.getName());
    }

    /**
     * Obtiene el nombre del usuario autenticado o lanza una excepción si no hay sesión.
     *
     * @return nombre del usuario autenticado
     * @throws AuthenticationCredentialsNotFoundException si no hay usuario autenticado
     */
    public String getRequiredUsername() {
        return getCurrentUsername()
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "No hay un usuario autenticado en la petición actual"));
    }
}

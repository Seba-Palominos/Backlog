package com.backlog.service.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Codificador de contraseñas personalizado.
 * Envuelve BCrypt para centralizar el algoritmo y la fuerza del hash
 * usado en todo el servicio de backlog.
 */
@Component
public class CustomPasswordEncoder implements PasswordEncoder {

    /** Fuerza del algoritmo BCrypt (número de rondas de hashing). */
    private static final int BCRYPT_STRENGTH = 12;

    private final BCryptPasswordEncoder delegate;

    public CustomPasswordEncoder() {
        this.delegate = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    /**
     * Genera el hash de una contraseña en texto plano.
     *
     * @param rawPassword contraseña en texto plano
     * @return hash seguro de la contraseña
     */
    @Override
    public String encode(CharSequence rawPassword) {
        validateRawPassword(rawPassword);
        return delegate.encode(rawPassword);
    }

    /**
     * Verifica si una contraseña en texto plano coincide con el hash almacenado.
     *
     * @param rawPassword     contraseña en texto plano
     * @param encodedPassword hash previamente almacenado
     * @return {@code true} si coinciden, {@code false} en caso contrario
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        return delegate.matches(rawPassword, encodedPassword);
    }

    /**
     * Valida que la contraseña en texto plano no sea nula ni vacía.
     *
     * @param rawPassword contraseña a validar
     */
    private void validateRawPassword(CharSequence rawPassword) {
        if (rawPassword == null || rawPassword.length() == 0) {
            throw new IllegalArgumentException("La contraseña no puede ser nula ni vacía");
        }
    }
}

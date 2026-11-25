package com.backlog.service.autenticacion.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta del inicio de sesión con el token generado y el usuario autenticado.
 * El nombre de usuario es necesario para el login con Google, donde el
 * formulario no lo conoce porque la cuenta puede haberse creado en ese momento.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String username;
}

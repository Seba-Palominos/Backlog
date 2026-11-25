package com.backlog.service.autenticacion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos necesarios para autenticar a un usuario con Google.
 * El frontend envía el ID token que devuelve Google Identity Services.
 */
@Getter
@Setter
@NoArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "La credencial de Google es obligatoria")
    private String credential;
}

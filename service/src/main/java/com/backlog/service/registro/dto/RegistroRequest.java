package com.backlog.service.registro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos recibidos desde el formulario de registro de usuario.
 */
@Getter
@Setter
@NoArgsConstructor
public class RegistroRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 100, message = "El nombre de usuario no puede superar los 100 caracteres")
    private String username;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    private String password;

    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    @JsonProperty("password_confirm")
    private String confirmPassword;
}

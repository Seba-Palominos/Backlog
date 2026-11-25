package com.backlog.service.elemento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos enviados para crear un directorio dentro de un repositorio.
 */
@Getter
@Setter
@NoArgsConstructor
public class DirectorioRequest {

    @NotBlank(message = "El nombre del directorio es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String titulo;

    private Long idElementoPadre;
}

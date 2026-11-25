package com.backlog.service.elemento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos enviados para crear una épica dentro de un repositorio.
 */
@Getter
@Setter
@NoArgsConstructor
public class EpicaRequest {

    @NotBlank(message = "El nombre de la épica es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String titulo;

    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
    private String descripcion;

    private Long idElementoPadre;
}

package com.backlog.service.proyecto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos enviados para crear un repositorio (proyecto) del workspace ágil.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProyectoRequest {

    @NotBlank(message = "El nombre del repositorio es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String titulo;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    private String descripcion;
}

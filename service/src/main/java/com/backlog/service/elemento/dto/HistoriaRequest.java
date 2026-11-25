package com.backlog.service.elemento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos enviados para crear una historia de usuario dentro de un repositorio.
 * El identificador funcional se recibe de la plantilla y se persiste tal cual.
 */
@Getter
@Setter
@NoArgsConstructor
public class HistoriaRequest {

    @NotBlank(message = "El identificador de la historia es obligatorio")
    @Size(max = 50, message = "El identificador no puede superar los 50 caracteres")
    private String identificador;

    @NotBlank(message = "El título de la historia es obligatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String titulo;

    private Long idEpicaPadre;

    private Long idElementoPadre;

    @Size(max = 4000, message = "Las reglas de negocio no pueden superar los 4000 caracteres")
    private String reglasNegocio;

    @Size(max = 4000, message = "Los criterios de aceptación no pueden superar los 4000 caracteres")
    private String criteriosAceptacion;

    @NotBlank(message = "El estado de la historia es obligatorio")
    @Size(max = 20, message = "El estado no puede superar los 20 caracteres")
    private String estado;
}

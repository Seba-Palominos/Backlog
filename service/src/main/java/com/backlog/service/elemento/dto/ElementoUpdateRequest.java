package com.backlog.service.elemento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos editable de un elemento del backlog. Los campos específicos se aplican
 * según el tipo de elemento que se esté modificando.
 */
@Getter
@Setter
@NoArgsConstructor
public class ElementoUpdateRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String titulo;

    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
    private String descripcion;

    @Size(max = 50, message = "El identificador no puede superar los 50 caracteres")
    private String identificador;

    @Size(max = 4000, message = "Las reglas de negocio no pueden superar los 4000 caracteres")
    private String reglasNegocio;

    @Size(max = 4000, message = "Los criterios de aceptación no pueden superar los 4000 caracteres")
    private String criteriosAceptacion;

    @Size(max = 20, message = "El estado no puede superar los 20 caracteres")
    private String estado;

    private Long idEpicaPadre;

    private Long idElementoPadre;

    @Size(max = 50, message = "El color no puede superar los 50 caracteres")
    private String color;

    @NotNull(message = "La versión del elemento es obligatoria")
    private Long version;
}

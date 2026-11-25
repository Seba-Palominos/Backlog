package com.backlog.service.elemento.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Datos de un elemento del backlog (sub-carpeta, épica o historia)
 * que se devuelven al dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ElementoResponse {

    private Long id;
    private String titulo;
    private String descripcion;
    private String tipo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltModificacion;
    private Long idElementoPadre;
    private String tituloPadre;
    private String identificador;
    private String reglasNegocio;
    private String criteriosAceptacion;
    private String estado;
    /** Épica asociada a la historia (relación EsAsociado). */
    private Long idEpica;
    private String tituloEpica;
    private String color;
    private Long version;
}

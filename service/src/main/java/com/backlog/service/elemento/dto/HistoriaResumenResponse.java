package com.backlog.service.elemento.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos mínimos de una historia de usuario asociada a una épica, usados en el
 * resumen del panel de información del repositorio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaResumenResponse {

    private Long id;
    private String identificador;
    private String titulo;
    private String estado;
}

package com.backlog.service.elemento.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Resumen de una épica del repositorio con las historias de usuario que tiene
 * asociadas. Se usa en el panel de información del detalle del repositorio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EpicaResumenResponse {

    private Long id;
    private String titulo;
    private Long idElementoPadre;
    private String tituloPadre;
    private long totalHistorias;
    private List<HistoriaResumenResponse> historias;
}

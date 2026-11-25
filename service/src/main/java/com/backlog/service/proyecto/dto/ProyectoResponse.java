package com.backlog.service.proyecto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Datos de un repositorio (proyecto) que se devuelven al dashboard.
 * Los contadores se calculan a partir de los elementos persistidos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProyectoResponse {

    private Long id;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaCreacion;
    private String propietario;
    private boolean propio;
    private long epicas;
    private long historias;
}

package com.backlog.service.elemento.entitys;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Clave compuesta de la relación EsAsociado (historia y épica).
 * Las columnas físicas las define cada asociación con @MapsId y @JoinColumn,
 * de modo que la clave y las claves foráneas compartan la misma columna.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EsAsociadoId implements Serializable {

    private Long storyId;

    private Long epicId;
}

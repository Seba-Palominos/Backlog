package com.backlog.service.proyecto.entitys;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Clave compuesta de la relación Participa (usuario y proyecto).
 * Las columnas físicas las define cada asociación con @MapsId y @JoinColumn,
 * de modo que la clave y las claves foráneas compartan la misma columna.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ParticipaId implements Serializable {

    private Long userId;

    private Long projectId;
}

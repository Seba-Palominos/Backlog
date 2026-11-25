package com.backlog.service.elemento.entitys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa un directorio o carpeta del backlog.
 * Corresponde a la tabla Directorio del modelo de base de datos.
 */
@Entity
@Table(name = "Directorio")
@PrimaryKeyJoinColumn(name = "idElemento")
@Getter
@Setter
@NoArgsConstructor
public class Directorio extends Elemento {

    @Column(name = "color")
    private String color;
}

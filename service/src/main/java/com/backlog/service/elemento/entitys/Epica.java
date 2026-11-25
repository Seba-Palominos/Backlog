package com.backlog.service.elemento.entitys;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa una épica del backlog.
 * Corresponde a la tabla Epica del modelo de base de datos.
 */
@Entity
@Table(name = "Epica")
@PrimaryKeyJoinColumn(name = "idElemento")
@Getter
@Setter
@NoArgsConstructor
public class Epica extends Elemento {
}

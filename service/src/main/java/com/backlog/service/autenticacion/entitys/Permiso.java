package com.backlog.service.autenticacion.entitys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa un tipo de permiso otorgable a un usuario.
 * Corresponde a la tabla Permiso del modelo de base de datos.
 */
@Entity
@Table(name = "Permiso")
@Getter
@Setter
@NoArgsConstructor
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPermiso")
    private Long id;

    @Column(name = "tipo")
    private String type;
}

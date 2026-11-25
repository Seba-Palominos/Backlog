package com.backlog.service.elemento.entitys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa una historia de usuario dentro del backlog.
 * Corresponde a la tabla HistoriaDeUsuario del modelo de base de datos.
 */
@Entity
@Table(name = "HistoriaDeUsuario")
@PrimaryKeyJoinColumn(name = "idElemento")
@Getter
@Setter
@NoArgsConstructor
public class HistoriaUsuario extends Elemento {

    @Column(name = "identificador", length = 50)
    private String identifier;

    @Column(name = "reglasNegocio", length = 4000)
    private String businessRules;

    @Column(name = "criteriosAceptacion", length = 4000)
    private String acceptanceCriteria;

    @Column(name = "estado", length = 20)
    private String status;
}

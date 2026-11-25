package com.backlog.service.elemento.entitys;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que asocia una historia de usuario con una épica.
 * Corresponde a la tabla EsAsociado del modelo de base de datos.
 */
@Entity
@Table(name = "EsAsociado")
@Getter
@Setter
@NoArgsConstructor
public class EsAsociado {

    @EmbeddedId
    private EsAsociadoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("storyId")
    @JoinColumn(name = "id_elemento_historia")
    private HistoriaUsuario story;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("epicId")
    @JoinColumn(name = "id_elemento_epica")
    private Epica epic;
}

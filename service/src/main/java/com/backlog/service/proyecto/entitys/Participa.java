package com.backlog.service.proyecto.entitys;

import com.backlog.service.registro.entitys.Usuario;
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
 * Entidad que representa la participación de un usuario en un proyecto.
 * Corresponde a la tabla Participa del modelo de base de datos.
 */
@Entity
@Table(name = "Participa")
@Getter
@Setter
@NoArgsConstructor
public class Participa {

    @EmbeddedId
    private ParticipaId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "id_usuario")
    private Usuario user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("projectId")
    @JoinColumn(name = "id_proyecto")
    private Proyecto project;
}

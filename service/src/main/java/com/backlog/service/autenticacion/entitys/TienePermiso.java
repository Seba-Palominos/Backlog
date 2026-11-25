package com.backlog.service.autenticacion.entitys;

import com.backlog.service.proyecto.entitys.Proyecto;
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
 * Entidad que representa la asignación de un permiso a un usuario.
 * Corresponde a la tabla TienePermiso del modelo de base de datos.
 */
@Entity
@Table(name = "TienePermiso")
@Getter
@Setter
@NoArgsConstructor
public class TienePermiso {

    @EmbeddedId
    private TienePermisoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "id_usuario")
    private Usuario user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("permissionId")
    @JoinColumn(name = "id_permiso")
    private Permiso permission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("projectId")
    @JoinColumn(name = "id_proyecto")
    private Proyecto project;
}

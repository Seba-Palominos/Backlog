package com.backlog.service.proyecto.entitys;

import com.backlog.service.registro.entitys.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad que representa un proyecto de backlog.
 * Corresponde a la tabla Proyecto del modelo de base de datos.
 */
@Entity
@Table(name = "Proyecto")
@Getter
@Setter
@NoArgsConstructor
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProyecto")
    private Long id;

    @Column(name = "titulo")
    private String title;

    @Column(name = "descripcion")
    private String description;

    @Column(name = "fechaCreacion")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuarioPropietario")
    private Usuario owner;
}

package com.backlog.service.elemento.entitys;

import com.backlog.service.proyecto.entitys.Proyecto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad base de todos los elementos de un backlog (directorios, épicas e
 * historias de usuario). Corresponde a la tabla Elemento del modelo de datos
 * y define la jerarquía mediante la autorreferencia al elemento padre.
 */
@Entity
@Table(name = "Elemento")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public class Elemento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idElemento")
    private Long id;

    @Column(name = "Titulo", length = 150)
    private String title;

    @Column(name = "descripcion", length = 2000)
    private String description;

    @Column(name = "fechaCreacion")
    private LocalDateTime createdAt;

    @Column(name = "fechaUltModificacion")
    private LocalDateTime lastModifiedAt;

    /**
     * Control de concurrencia optimista: Hibernate incrementa este valor en cada
     * modificación y rechaza la operación si otra transacción ya cambió el elemento.
     */
    @Version
    @Column(name = "version", columnDefinition = "BIGINT DEFAULT 0")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idProyecto")
    private Proyecto project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idElementoPadre")
    private Elemento parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Elemento> children = new ArrayList<>();
}

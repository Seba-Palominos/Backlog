package com.backlog.service.registro.entitys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entidad que representa a un usuario registrado en el sistema de backlog.
 * Corresponde a la tabla Usuario del modelo de base de datos.
 */
@Entity
@Table(name = "Usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUsuario")
    private Long id;

    @Column(name = "nombreUsuario")
    private String username;

    @Column(name = "contraseña")
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "fechaNacimiento")
    private LocalDate birthDate;
}

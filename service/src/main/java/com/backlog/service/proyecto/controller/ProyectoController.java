package com.backlog.service.proyecto.controller;

import com.backlog.service.proyecto.dto.ProyectoRequest;
import com.backlog.service.proyecto.dto.ProyectoResponse;
import com.backlog.service.proyecto.services.ProyectoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador de los repositorios (proyectos) que se muestran en el dashboard.
 */
@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    @Autowired
    private ProyectoService proyectoService;

    /**
     * Lista los repositorios creados por el usuario autenticado.
     *
     * @return repositorios propios
     */
    @GetMapping
    public ResponseEntity<List<ProyectoResponse>> listOwned() {
        return ResponseEntity.ok(proyectoService.listOwned());
    }

    /**
     * Lista los repositorios compartidos con el usuario autenticado.
     *
     * @return repositorios compartidos
     */
    @GetMapping("/compartidos")
    public ResponseEntity<List<ProyectoResponse>> listShared() {
        return ResponseEntity.ok(proyectoService.listShared());
    }

    /**
     * Obtiene un repositorio accesible para el usuario autenticado.
     *
     * @param projectId identificador del repositorio
     * @return repositorio solicitado
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProyectoResponse> getById(@PathVariable Long projectId) {
        return ResponseEntity.ok(proyectoService.getById(projectId));
    }

    /**
     * Crea un repositorio para el usuario autenticado.
     *
     * @param request datos del repositorio
     * @return repositorio creado
     */
    @PostMapping
    public ResponseEntity<ProyectoResponse> create(@Valid @RequestBody ProyectoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proyectoService.create(request));
    }

    /**
     * Elimina un repositorio junto con todos sus elementos de forma recursiva.
     *
     * @param projectId identificador del repositorio
     * @return respuesta sin contenido
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId) {
        proyectoService.delete(projectId);
        return ResponseEntity.noContent().build();
    }
}

package com.backlog.service.elemento.controller;

import com.backlog.service.elemento.dto.DirectorioRequest;
import com.backlog.service.elemento.dto.ElementoResponse;
import com.backlog.service.elemento.dto.ElementoUpdateRequest;
import com.backlog.service.elemento.dto.EpicaRequest;
import com.backlog.service.elemento.dto.EpicaResumenResponse;
import com.backlog.service.elemento.dto.HistoriaRequest;
import com.backlog.service.elemento.services.proxy.ElementoAccessProxy;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador de los elementos del backlog asociados a un repositorio.
 */
@RestController
@RequestMapping("/api/proyectos/{projectId}")
public class ElementoController {

    @Autowired
    private ElementoAccessProxy elementoAccessProxy;

    /**
     * Lista los elementos de un repositorio.
     *
     * @param projectId identificador del repositorio
     * @return elementos del repositorio
     */
    @GetMapping("/elementos")
    public ResponseEntity<List<ElementoResponse>> list(
            @PathVariable Long projectId,
            @RequestParam(name = "parentId", required = false) Long parentId) {
        return ResponseEntity.ok(parentId == null
                ? elementoAccessProxy.listByProject(projectId)
                : elementoAccessProxy.listByParent(projectId, parentId));
    }

    /**
     * Lista todas las épicas de un repositorio, sin importar su directorio.
     *
     * @param projectId identificador del repositorio
     * @return épicas del repositorio
     */
    @GetMapping("/epicas")
    public ResponseEntity<List<ElementoResponse>> listEpics(@PathVariable Long projectId) {
        return ResponseEntity.ok(elementoAccessProxy.listEpicsByProject(projectId));
    }

    /**
     * Lista el resumen de las épicas del repositorio con sus historias asociadas.
     *
     * @param projectId identificador del repositorio
     * @return resumen de las épicas del repositorio
     */
    @GetMapping("/epicas/resumen")
    public ResponseEntity<List<EpicaResumenResponse>> listEpicSummary(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(elementoAccessProxy.listEpicSummary(projectId));
    }

    /**
     * Obtiene el detalle de un elemento del repositorio.
     *
     * @param projectId identificador del repositorio
     * @param elementId identificador del elemento
     * @return datos del elemento
     */
    @GetMapping("/elementos/{elementId}")
    public ResponseEntity<ElementoResponse> getById(
            @PathVariable Long projectId,
            @PathVariable Long elementId) {
        return ResponseEntity.ok(elementoAccessProxy.getById(projectId, elementId));
    }

    /**
     * Modifica un elemento del repositorio.
     *
     * @param projectId identificador del repositorio
     * @param elementId identificador del elemento
     * @param request datos a actualizar
     * @return elemento actualizado
     */
    @PutMapping("/elementos/{elementId}")
    public ResponseEntity<ElementoResponse> update(
            @PathVariable Long projectId,
            @PathVariable Long elementId,
            @Valid @RequestBody ElementoUpdateRequest request) {
        return ResponseEntity.ok(elementoAccessProxy.updateElement(projectId, elementId, request));
    }

    /**
     * Elimina un elemento del repositorio.
     *
     * @param projectId identificador del repositorio
     * @param elementId identificador del elemento
     * @return respuesta sin contenido
     */
    @DeleteMapping("/elementos/{elementId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long elementId) {
        elementoAccessProxy.deleteElement(projectId, elementId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Crea un directorio dentro del repositorio.
     *
     * @param projectId identificador del repositorio
     * @param request datos del directorio
     * @return directorio creado
     */
    @PostMapping("/directorios")
    public ResponseEntity<ElementoResponse> createDirectory(
            @PathVariable Long projectId,
            @Valid @RequestBody DirectorioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(elementoAccessProxy.createDirectory(projectId, request));
    }

    /**
     * Crea una épica dentro del repositorio.
     *
     * @param projectId identificador del repositorio
     * @param request datos de la épica
     * @return épica creada
     */
    @PostMapping("/epicas")
    public ResponseEntity<ElementoResponse> createEpic(
            @PathVariable Long projectId,
            @Valid @RequestBody EpicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(elementoAccessProxy.createEpic(projectId, request));
    }

    /**
     * Crea una historia de usuario dentro del repositorio.
     *
     * @param projectId identificador del repositorio
     * @param request datos de la historia de usuario
     * @return historia creada
     */
    @PostMapping("/historias")
    public ResponseEntity<ElementoResponse> createStory(
            @PathVariable Long projectId,
            @Valid @RequestBody HistoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(elementoAccessProxy.createStory(projectId, request));
    }
}

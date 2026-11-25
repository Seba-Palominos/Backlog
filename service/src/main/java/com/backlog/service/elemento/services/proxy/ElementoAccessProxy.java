package com.backlog.service.elemento.services.proxy;

import com.backlog.service.autenticacion.entitys.PermisoAccion;
import com.backlog.service.autenticacion.services.PermisoService;
import com.backlog.service.elemento.dto.DirectorioRequest;
import com.backlog.service.elemento.dto.ElementoResponse;
import com.backlog.service.elemento.dto.ElementoUpdateRequest;
import com.backlog.service.elemento.dto.EpicaRequest;
import com.backlog.service.elemento.dto.EpicaResumenResponse;
import com.backlog.service.elemento.dto.HistoriaRequest;
import com.backlog.service.elemento.services.ElementoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Proxy que controla los permisos del usuario sobre el proyecto antes de
 * delegar en el servicio de elementos. La consulta requiere lectura, las altas
 * requieren creación, la edición requiere modificación y el borrado eliminación.
 */
@Component
public class ElementoAccessProxy {

    @Autowired
    private ElementoService elementoService;

    @Autowired
    private PermisoService permisoService;

    /**
     * Lista los elementos de un proyecto si el usuario puede leerlos.
     *
     * @param projectId identificador del repositorio
     * @return elementos visibles del repositorio
     */
    public List<ElementoResponse> listByProject(Long projectId) {
        permisoService.requirePermission(projectId, PermisoAccion.LEER);
        return elementoService.listByProject(projectId);
    }

    /**
     * Lista los hijos de un directorio si el usuario puede leer los elementos.
     *
     * @param projectId identificador del repositorio
     * @param parentId identificador del directorio padre
     * @return elementos contenidos en el directorio
     */
    public List<ElementoResponse> listByParent(Long projectId, Long parentId) {
        permisoService.requirePermission(projectId, PermisoAccion.LEER);
        return elementoService.listByParent(projectId, parentId);
    }

    /**
     * Lista todas las épicas del proyecto si el usuario puede leer los elementos.
     *
     * @param projectId identificador del repositorio
     * @return épicas del repositorio
     */
    public List<ElementoResponse> listEpicsByProject(Long projectId) {
        permisoService.requirePermission(projectId, PermisoAccion.LEER);
        return elementoService.listEpicsByProject(projectId);
    }

    /**
     * Lista el resumen de las épicas con sus historias asociadas si el usuario
     * puede leer los elementos del proyecto.
     *
     * @param projectId identificador del repositorio
     * @return resumen de las épicas del repositorio
     */
    public List<EpicaResumenResponse> listEpicSummary(Long projectId) {
        permisoService.requirePermission(projectId, PermisoAccion.LEER);
        return elementoService.listEpicSummary(projectId);
    }

    /**
     * Obtiene un elemento si el usuario puede leer los elementos del proyecto.
     *
     * @param projectId identificador del repositorio
     * @param elementId identificador del elemento
     * @return datos del elemento
     */
    public ElementoResponse getById(Long projectId, Long elementId) {
        permisoService.requirePermission(projectId, PermisoAccion.LEER);
        return elementoService.getById(projectId, elementId);
    }

    /**
     * Modifica un elemento si el usuario puede modificar elementos.
     *
     * @param projectId identificador del repositorio
     * @param elementId identificador del elemento
     * @param request datos a actualizar
     * @return elemento actualizado
     */
    public ElementoResponse updateElement(Long projectId, Long elementId,
            ElementoUpdateRequest request) {
        permisoService.requirePermission(projectId, PermisoAccion.MODIFICAR);
        return elementoService.updateElement(projectId, elementId, request);
    }

    /**
     * Elimina un elemento si el usuario puede eliminar elementos.
     *
     * @param projectId identificador del repositorio
     * @param elementId identificador del elemento
     */
    public void deleteElement(Long projectId, Long elementId) {
        permisoService.requirePermission(projectId, PermisoAccion.ELIMINAR);
        elementoService.deleteElement(projectId, elementId);
    }

    /**
     * Crea un directorio si el usuario puede crear elementos.
     *
     * @param projectId identificador del repositorio
     * @param request datos del directorio
     * @return directorio creado
     */
    public ElementoResponse createDirectory(Long projectId, DirectorioRequest request) {
        permisoService.requirePermission(projectId, PermisoAccion.CREAR);
        return elementoService.createDirectory(projectId, request);
    }

    /**
     * Crea una épica si el usuario puede crear elementos.
     *
     * @param projectId identificador del repositorio
     * @param request datos de la épica
     * @return épica creada
     */
    public ElementoResponse createEpic(Long projectId, EpicaRequest request) {
        permisoService.requirePermission(projectId, PermisoAccion.CREAR);
        return elementoService.createEpic(projectId, request);
    }

    /**
     * Crea una historia si el usuario puede crear elementos.
     *
     * @param projectId identificador del repositorio
     * @param request datos de la historia
     * @return historia creada
     */
    public ElementoResponse createStory(Long projectId, HistoriaRequest request) {
        permisoService.requirePermission(projectId, PermisoAccion.CREAR);
        return elementoService.createStory(projectId, request);
    }
}

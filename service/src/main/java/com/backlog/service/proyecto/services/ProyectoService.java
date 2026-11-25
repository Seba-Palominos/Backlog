package com.backlog.service.proyecto.services;

import com.backlog.service.elemento.composite.BacklogDeletionService;
import com.backlog.service.elemento.repository.EpicaRepository;
import com.backlog.service.elemento.repository.HistoriaUsuarioRepository;
import com.backlog.service.autenticacion.services.PermisoService;
import com.backlog.service.exception.PermisoDenegadoException;
import com.backlog.service.proyecto.dto.ProyectoRequest;
import com.backlog.service.proyecto.dto.ProyectoResponse;
import com.backlog.service.proyecto.entitys.Participa;
import com.backlog.service.proyecto.entitys.Proyecto;
import com.backlog.service.proyecto.repository.ParticipaRepository;
import com.backlog.service.proyecto.repository.ProyectoRepository;
import com.backlog.service.registro.entitys.Usuario;
import com.backlog.service.registro.repository.UsuarioRepository;
import com.backlog.service.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio con la lógica de negocio de los repositorios (proyectos) ágiles.
 */
@Service
public class ProyectoService {

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private ParticipaRepository participaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EpicaRepository epicaRepository;

    @Autowired
    private HistoriaUsuarioRepository historiaUsuarioRepository;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private PermisoService permisoService;

    @Autowired
    private BacklogDeletionService backlogDeletionService;

    /**
     * Crea un repositorio cuyo propietario es el usuario autenticado.
     *
     * @param request datos del repositorio
     * @return el repositorio persistido con sus contadores
     */
    @Transactional
    public ProyectoResponse create(ProyectoRequest request) {
        if (request == null || request.getTitulo() == null || request.getTitulo().isBlank()) {
            throw new IllegalArgumentException("El nombre del repositorio es obligatorio");
        }

        Usuario owner = getCurrentUser();

        Proyecto proyecto = new Proyecto();
        proyecto.setTitle(request.getTitulo().trim());
        proyecto.setDescription(trimToNull(request.getDescripcion()));
        proyecto.setCreatedAt(LocalDateTime.now());
        proyecto.setOwner(owner);

        Proyecto saved = proyectoRepository.save(proyecto);
        return toResponse(saved, true);
    }

    /**
     * Lista los repositorios creados por el usuario autenticado.
     *
     * @return repositorios propios
     */
    @Transactional(readOnly = true)
    public List<ProyectoResponse> listOwned() {
        Usuario user = getCurrentUser();
        return proyectoRepository.findByOwner_Id(user.getId()).stream()
                .map(proyecto -> toResponse(proyecto, true))
                .toList();
    }

    /**
     * Lista los repositorios en los que el usuario participa sin ser propietario.
     *
     * @return repositorios compartidos con el usuario
     */
    @Transactional(readOnly = true)
    public List<ProyectoResponse> listShared() {
        Usuario user = getCurrentUser();
        return participaRepository.findByIdUserId(user.getId()).stream()
                .map(Participa::getProject)
                .filter(proyecto -> !proyecto.getOwner().getId().equals(user.getId()))
                .distinct()
                .map(proyecto -> toResponse(proyecto, false))
                .toList();
    }

    /**
     * Obtiene un repositorio accesible con sus contadores.
     *
     * @param projectId identificador del repositorio
     * @return datos del repositorio
     */
    @Transactional(readOnly = true)
    public ProyectoResponse getById(Long projectId) {
        Proyecto proyecto = getAccessibleProject(projectId);
        Usuario user = getCurrentUser();
        boolean propio = proyecto.getOwner().getId().equals(user.getId());
        return toResponse(proyecto, propio);
    }

    /**
     * Obtiene un repositorio verificando que el usuario sea propietario, participante
     * o que tenga permisos asignados sobre el proyecto.
     *
     * @param projectId identificador del repositorio
     * @return el repositorio accesible
     * @throws IllegalArgumentException si no existe o el usuario no tiene acceso
     */
    public Proyecto getAccessibleProject(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("El repositorio es obligatorio");
        }

        Usuario user = getCurrentUser();
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("El repositorio no existe"));

        boolean isOwner = proyecto.getOwner().getId().equals(user.getId());
        boolean isParticipant = participaRepository
                .existsByIdUserIdAndIdProjectId(user.getId(), projectId);
        boolean hasPermission = permisoService.hasAnyPermission(user, projectId);

        if (!isOwner && !isParticipant && !hasPermission) {
            throw new IllegalArgumentException("No tienes acceso a este repositorio");
        }

        return proyecto;
    }

    /**
     * Elimina un repositorio y todos sus elementos de forma recursiva.
     *
     * @param projectId identificador del repositorio
     */
    @Transactional
    public void delete(Long projectId) {
        Proyecto proyecto = getAccessibleProject(projectId);
        Usuario user = getCurrentUser();
        if (!proyecto.getOwner().getId().equals(user.getId())) {
            throw new PermisoDenegadoException("Solo el propietario puede eliminar el repositorio");
        }
        backlogDeletionService.deleteProjectTree(proyecto);
    }

    /**
     * Recupera el usuario autenticado a partir del nombre del token.
     *
     * @return usuario autenticado
     */
    private Usuario getCurrentUser() {
        String username = currentUserProvider.getRequiredUsername();
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("El usuario autenticado no existe"));
    }

    /**
     * Convierte la entidad en el DTO que consume el dashboard.
     *
     * @param proyecto repositorio persistido
     * @param propio indica si el usuario autenticado es su propietario
     * @return datos del repositorio con contadores
     */
    private ProyectoResponse toResponse(Proyecto proyecto, boolean propio) {
        ProyectoResponse response = new ProyectoResponse();
        response.setId(proyecto.getId());
        response.setTitulo(proyecto.getTitle());
        response.setDescripcion(proyecto.getDescription());
        response.setFechaCreacion(proyecto.getCreatedAt());
        response.setPropietario(proyecto.getOwner().getUsername());
        response.setPropio(propio);
        response.setEpicas(epicaRepository.countByProject_Id(proyecto.getId()));
        response.setHistorias(historiaUsuarioRepository.countByProject_Id(proyecto.getId()));
        return response;
    }

    /**
     * Normaliza un texto opcional eliminando espacios sobrantes.
     *
     * @param value texto de entrada
     * @return texto limpio o {@code null} si queda vacío
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

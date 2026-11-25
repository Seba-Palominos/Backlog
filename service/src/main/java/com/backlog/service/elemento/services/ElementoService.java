package com.backlog.service.elemento.services;

import com.backlog.service.exception.ConflictoConcurrenciaException;
import com.backlog.service.elemento.composite.BacklogDeletionService;
import com.backlog.service.elemento.dto.DirectorioRequest;
import com.backlog.service.elemento.dto.ElementoResponse;
import com.backlog.service.elemento.dto.ElementoUpdateRequest;
import com.backlog.service.elemento.dto.EpicaRequest;
import com.backlog.service.elemento.dto.EpicaResumenResponse;
import com.backlog.service.elemento.dto.HistoriaRequest;
import com.backlog.service.elemento.dto.HistoriaResumenResponse;
import com.backlog.service.elemento.entitys.Directorio;
import com.backlog.service.elemento.entitys.Elemento;
import com.backlog.service.elemento.entitys.Epica;
import com.backlog.service.elemento.entitys.EsAsociado;
import com.backlog.service.elemento.entitys.EsAsociadoId;
import com.backlog.service.elemento.entitys.HistoriaUsuario;
import com.backlog.service.elemento.repository.DirectorioRepository;
import com.backlog.service.elemento.repository.ElementoRepository;
import com.backlog.service.elemento.repository.EpicaRepository;
import com.backlog.service.elemento.repository.EsAsociadoRepository;
import com.backlog.service.elemento.repository.HistoriaUsuarioRepository;
import com.backlog.service.proyecto.entitys.Proyecto;
import com.backlog.service.proyecto.services.ProyectoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Servicio con la lógica de negocio de los elementos del backlog:
 * consulta, creación, modificación y eliminación de directorios, épicas e historias.
 */
@Service
public class ElementoService {

    private static final Set<String> ALLOWED_STORY_STATUSES =
            Set.of("aprobada", "rechazada", "en desarrollo");

    @Autowired
    private DirectorioRepository directorioRepository;

    @Autowired
    private EpicaRepository epicaRepository;

    @Autowired
    private HistoriaUsuarioRepository historiaUsuarioRepository;

    @Autowired
    private EsAsociadoRepository esAsociadoRepository;

    @Autowired
    private ElementoRepository elementoRepository;

    @Autowired
    private BacklogDeletionService backlogDeletionService;

    @Autowired
    private ProyectoService proyectoService;

    /**
     * Lista los elementos directos de un repositorio accesible para el usuario.
     * El repositorio se comporta como el directorio raíz: no incluye nietos.
     *
     * @param projectId identificador del repositorio
     * @return elementos contenidos directamente en el repositorio
     */
    @Transactional(readOnly = true)
    public List<ElementoResponse> listByProject(Long projectId) {
        Proyecto proyecto = proyectoService.getAccessibleProject(projectId);

        List<Epica> epicas = epicaRepository.findByProject_Id(proyecto.getId());
        Map<Long, Epica> epicsById = indexEpics(epicas);
        Map<Long, Epica> associatedEpics = loadAssociatedEpics(proyecto.getId());

        // Se consulta cada subtipo por separado para conservar su tipo polimórfico
        // aunque el repositorio base devuelva entidades sin discriminar.
        List<ElementoResponse> elements = new ArrayList<>();
        directorioRepository.findByProject_Id(proyecto.getId()).stream()
                .filter(elemento -> elemento.getParent() == null)
                .forEach(directorio -> elements.add(toResponse(directorio)));
        epicas.stream()
                .filter(elemento -> elemento.getParent() == null)
                .forEach(epica -> elements.add(toResponse(epica)));

        // Las historias se ubican por su directorio padre, nunca por su épica.
        for (HistoriaUsuario historia : historiaUsuarioRepository.findByProject_Id(proyecto.getId())) {
            StoryContext context = resolveStoryContext(historia, epicsById, associatedEpics);
            if (context.parentId() == null) {
                elements.add(toResponse(historia, context));
            }
        }

        return elements.stream()
                .sorted(Comparator.comparing(ElementoResponse::getId))
                .toList();
    }

    /**
     * Lista los elementos hijos directos de un directorio del repositorio.
     *
     * @param projectId identificador del repositorio
     * @param parentId identificador del directorio padre
     * @return elementos contenidos directamente en el directorio
     */
    @Transactional(readOnly = true)
    public List<ElementoResponse> listByParent(Long projectId, Long parentId) {
        Proyecto proyecto = proyectoService.getAccessibleProject(projectId);
        if (!(findElementInProject(projectId, parentId) instanceof Directorio)) {
            throw new IllegalArgumentException("El elemento indicado no es un directorio");
        }

        List<Epica> epicas = epicaRepository.findByProject_Id(proyecto.getId());
        Map<Long, Epica> epicsById = indexEpics(epicas);
        Map<Long, Epica> associatedEpics = loadAssociatedEpics(proyecto.getId());

        List<ElementoResponse> elements = new ArrayList<>();
        directorioRepository.findByProject_Id(proyecto.getId()).stream()
                .filter(elemento -> belongsTo(elemento, parentId))
                .forEach(elemento -> elements.add(toResponse(elemento)));
        epicas.stream()
                .filter(elemento -> belongsTo(elemento, parentId))
                .forEach(elemento -> elements.add(toResponse(elemento)));

        // Las historias se ubican por su directorio padre, nunca por su épica.
        for (HistoriaUsuario historia : historiaUsuarioRepository.findByProject_Id(proyecto.getId())) {
            StoryContext context = resolveStoryContext(historia, epicsById, associatedEpics);
            if (parentId.equals(context.parentId())) {
                elements.add(toResponse(historia, context));
            }
        }

        return elements.stream()
                .sorted(Comparator.comparing(ElementoResponse::getId))
                .toList();
    }

    /**
     * Lista todas las épicas de un repositorio sin importar el directorio en el
     * que se encuentren. Se usa para asociar cualquier épica del proyecto a una
     * historia de usuario.
     *
     * @param projectId identificador del repositorio
     * @return épicas del repositorio
     */
    @Transactional(readOnly = true)
    public List<ElementoResponse> listEpicsByProject(Long projectId) {
        Proyecto proyecto = proyectoService.getAccessibleProject(projectId);
        return epicaRepository.findByProject_Id(proyecto.getId()).stream()
                .sorted(Comparator.comparing(Epica::getId))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lista el resumen de cada épica del repositorio con las historias de
     * usuario que tiene asociadas. Se calcula sobre todo el proyecto, sin
     * importar el directorio que se esté consultando.
     *
     * @param projectId identificador del repositorio
     * @return resumen de las épicas con sus historias asociadas
     */
    @Transactional(readOnly = true)
    public List<EpicaResumenResponse> listEpicSummary(Long projectId) {
        Proyecto proyecto = proyectoService.getAccessibleProject(projectId);
        List<Epica> epicas = epicaRepository.findByProject_Id(proyecto.getId());
        Map<Long, Epica> epicsById = indexEpics(epicas);
        Map<Long, Epica> associatedEpics = loadAssociatedEpics(proyecto.getId());

        // Se agrupan las historias por épica usando la asociación actual y los
        // datos antiguos que guardaban la épica como padre jerárquico.
        Map<Long, List<HistoriaUsuario>> storiesByEpic = new HashMap<>();
        for (HistoriaUsuario historia : historiaUsuarioRepository.findByProject_Id(proyecto.getId())) {
            Epica epic = resolveEpicForStory(historia, epicsById, associatedEpics);
            if (epic != null) {
                storiesByEpic.computeIfAbsent(epic.getId(), key -> new ArrayList<>()).add(historia);
            }
        }

        return epicas.stream()
                .sorted(Comparator.comparing(Epica::getId))
                .map(epica -> toEpicSummary(epica, storiesByEpic.getOrDefault(epica.getId(), List.of())))
                .toList();
    }

    /**
     * Obtiene un elemento para su consulta detallada.
     *
     * @param projectId identificador del repositorio
     * @param elementId identificador del elemento
     * @return datos del elemento
     */
    @Transactional(readOnly = true)
    public ElementoResponse getById(Long projectId, Long elementId) {
        proyectoService.getAccessibleProject(projectId);
        Elemento elemento = findElementInProject(projectId, elementId);

        if (elemento instanceof HistoriaUsuario historia) {
            List<Epica> epicas = epicaRepository.findByProject_Id(projectId);
            StoryContext context = resolveStoryContext(
                    historia, indexEpics(epicas), loadAssociatedEpics(projectId));
            return toResponse(historia, context);
        }

        return toResponse(elemento);
    }

    /**
     * Modifica un elemento conservando los campos no enviados.
     *
     * @param projectId identificador del repositorio
     * @param elementId identificador del elemento
     * @param request datos a actualizar
     * @return elemento actualizado
     */
    @Transactional
    public ElementoResponse updateElement(Long projectId, Long elementId,
            ElementoUpdateRequest request) {
        proyectoService.getAccessibleProject(projectId);
        Elemento elemento = findElementInProject(projectId, elementId);

        Long currentVersion = elemento.getVersion() == null ? 0L : elemento.getVersion();
        if (request.getVersion() == null || !currentVersion.equals(request.getVersion())) {
            throw new ConflictoConcurrenciaException(
                    "El elemento fue modificado por otro usuario. Recarga la página para ver los cambios.");
        }

        elemento.setTitle(requireTitle(request.getTitulo()));
        if (request.getDescripcion() != null) {
            elemento.setDescription(trimToNull(request.getDescripcion()));
        }

        Epica associatedEpic = null;
        if (elemento instanceof HistoriaUsuario historia) {
            String identifier = trimToNull(request.getIdentificador());
            if (identifier != null) {
                historia.setIdentifier(requireIdentifier(identifier));
            }
            historia.setBusinessRules(trimToNull(request.getReglasNegocio()));
            historia.setAcceptanceCriteria(trimToNull(request.getCriteriosAceptacion()));
            String status = trimToNull(request.getEstado());
            if (status != null) {
                historia.setStatus(normalizeStatus(status));
            }
            // El padre jerárquico es solo el directorio; la épica se guarda como
            // asociación en EsAsociado para que la historia siga en el listado.
            historia.setParent(resolveParentDirectory(projectId, request.getIdElementoPadre()));
            associatedEpic = resolveParentEpic(projectId, request.getIdEpicaPadre());
            syncStoryEpicAssociation(historia, associatedEpic);
        } else if (elemento instanceof Directorio directorio) {
            directorio.setColor(trimToNull(request.getColor()));
        }

        elemento.setLastModifiedAt(LocalDateTime.now());
        Elemento guardado = elementoRepository.save(elemento);

        if (guardado instanceof HistoriaUsuario historia) {
            return toResponse(historia, contextFor(historia, associatedEpic));
        }
        return toResponse(guardado);
    }

    /**
     * Elimina un elemento del repositorio propagando el borrado a sus hijos.
     *
     * @param projectId identificador del repositorio
     * @param elementId identificador del elemento
     */
    @Transactional
    public void deleteElement(Long projectId, Long elementId) {
        proyectoService.getAccessibleProject(projectId);
        Elemento elemento = findElementInProject(projectId, elementId);
        backlogDeletionService.deleteElementTree(elemento);
    }

    /**
     * Crea un directorio dentro de un repositorio.
     *
     * @param projectId identificador del repositorio
     * @param request datos del directorio
     * @return directorio creado
     */
    @Transactional
    public ElementoResponse createDirectory(Long projectId, DirectorioRequest request) {
        Proyecto proyecto = proyectoService.getAccessibleProject(projectId);

        Directorio directorio = new Directorio();
        directorio.setTitle(requireTitle(request.getTitulo()));
        directorio.setProject(proyecto);
        directorio.setParent(resolveParentDirectory(projectId, request.getIdElementoPadre()));
        directorio.setCreatedAt(LocalDateTime.now());
        directorio.setLastModifiedAt(LocalDateTime.now());

        return toResponse(directorioRepository.save(directorio));
    }

    /**
     * Crea una épica dentro de un repositorio.
     *
     * @param projectId identificador del repositorio
     * @param request datos de la épica
     * @return épica creada
     */
    @Transactional
    public ElementoResponse createEpic(Long projectId, EpicaRequest request) {
        Proyecto proyecto = proyectoService.getAccessibleProject(projectId);

        Epica epica = new Epica();
        epica.setTitle(requireTitle(request.getTitulo()));
        epica.setDescription(trimToNull(request.getDescripcion()));
        epica.setProject(proyecto);
        epica.setParent(resolveParentDirectory(projectId, request.getIdElementoPadre()));
        epica.setCreatedAt(LocalDateTime.now());
        epica.setLastModifiedAt(LocalDateTime.now());

        return toResponse(epicaRepository.save(epica));
    }

    /**
     * Crea una historia de usuario dentro de un repositorio y, si se indica,
     * la vincula a una épica del mismo repositorio.
     *
     * @param projectId identificador del repositorio
     * @param request datos de la historia de usuario
     * @return historia creada
     */
    @Transactional
    public ElementoResponse createStory(Long projectId, HistoriaRequest request) {
        Proyecto proyecto = proyectoService.getAccessibleProject(projectId);

        HistoriaUsuario historia = new HistoriaUsuario();
        historia.setTitle(requireTitle(request.getTitulo()));
        historia.setIdentifier(requireIdentifier(request.getIdentificador()));
        historia.setBusinessRules(trimToNull(request.getReglasNegocio()));
        historia.setAcceptanceCriteria(trimToNull(request.getCriteriosAceptacion()));
        historia.setStatus(normalizeStatus(request.getEstado()));
        historia.setProject(proyecto);
        // El padre jerárquico es solo el directorio; la épica es una asociación.
        historia.setParent(resolveParentDirectory(projectId, request.getIdElementoPadre()));
        Epica associatedEpic = resolveParentEpic(projectId, request.getIdEpicaPadre());
        historia.setCreatedAt(LocalDateTime.now());
        historia.setLastModifiedAt(LocalDateTime.now());

        HistoriaUsuario guardada = historiaUsuarioRepository.save(historia);
        syncStoryEpicAssociation(guardada, associatedEpic);

        return toResponse(guardada, contextFor(guardada, associatedEpic));
    }

    /**
     * Busca un elemento por su identificador verificando que pertenezca al proyecto.
     * Se consulta cada subtipo por separado para conservar su tipo polimórfico.
     *
     * @param projectId identificador del repositorio
     * @param elementId identificador del elemento
     * @return elemento persistido
     */
    private Elemento findElementInProject(Long projectId, Long elementId) {
        if (elementId == null) {
            throw new IllegalArgumentException("El elemento es obligatorio");
        }

        Optional<Elemento> elemento = directorioRepository.findById(elementId)
                .map(directorio -> (Elemento) directorio);
        if (elemento.isEmpty()) {
            elemento = epicaRepository.findById(elementId).map(epica -> (Elemento) epica);
        }
        if (elemento.isEmpty()) {
            elemento = historiaUsuarioRepository.findById(elementId)
                    .map(historia -> (Elemento) historia);
        }

        Elemento encontrado = elemento.orElseThrow(
                () -> new IllegalArgumentException("El elemento no existe"));
        if (!encontrado.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("El elemento no pertenece a este repositorio");
        }
        return encontrado;
    }

    /**
     * Indica si el elemento es hijo directo del directorio indicado.
     *
     * @param elemento elemento a comprobar
     * @param parentId identificador del directorio padre
     * @return {@code true} cuando el elemento está contenido en el directorio
     */
    private boolean belongsTo(Elemento elemento, Long parentId) {
        return elemento.getParent() != null && parentId.equals(elemento.getParent().getId());
    }

    /**
     * Resuelve el directorio padre verificando que pertenezca al repositorio.
     *
     * @param projectId identificador del repositorio
     * @param parentId identificador del directorio recibido
     * @return directorio padre o {@code null} si no se indicó
     */
    private Directorio resolveParentDirectory(Long projectId, Long parentId) {
        if (parentId == null) {
            return null;
        }

        Elemento padre = findElementInProject(projectId, parentId);
        if (!(padre instanceof Directorio directorio)) {
            throw new IllegalArgumentException("El elemento padre debe ser un directorio");
        }
        return directorio;
    }

    /**
     * Resuelve la épica padre verificando que pertenezca al repositorio.
     *
     * @param projectId identificador del repositorio
     * @param epicId identificador de la épica recibida
     * @return épica padre o {@code null} si no se indicó
     */
    private Epica resolveParentEpic(Long projectId, Long epicId) {
        if (epicId == null) {
            return null;
        }

        Epica epica = epicaRepository.findById(epicId)
                .orElseThrow(() -> new IllegalArgumentException("La épica seleccionada no existe"));

        if (!epica.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("La épica seleccionada no pertenece a este repositorio");
        }

        return epica;
    }

    /**
     * Valida y normaliza el estado de la historia.
     *
     * @param status estado recibido
     * @return estado permitido en minúsculas
     */
    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase();

        if (!ALLOWED_STORY_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "El estado debe ser aprobada, rechazada o en desarrollo");
        }

        return normalized;
    }

    /**
     * Valida que el identificador de la plantilla no esté vacío.
     *
     * @param identifier identificador recibido
     * @return identificador normalizado
     */
    private String requireIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("El identificador es obligatorio");
        }
        return identifier.trim();
    }

    /**
     * Valida que el título no esté vacío.
     *
     * @param title título recibido
     * @return título normalizado
     */
    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        return title.trim();
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

    /**
     * Indexa las épicas del repositorio por su identificador.
     *
     * @param epicas épicas del repositorio
     * @return mapa de épicas por id
     */
    private Map<Long, Epica> indexEpics(List<Epica> epicas) {
        Map<Long, Epica> index = new HashMap<>();
        for (Epica epica : epicas) {
            index.put(epica.getId(), epica);
        }
        return index;
    }

    /**
     * Carga la épica asociada a cada historia del repositorio.
     *
     * @param projectId identificador del repositorio
     * @return mapa de historia a épica asociada
     */
    private Map<Long, Epica> loadAssociatedEpics(Long projectId) {
        Map<Long, Epica> associatedEpics = new HashMap<>();
        for (EsAsociado association : esAsociadoRepository.findByProjectId(projectId)) {
            associatedEpics.put(association.getStory().getId(), association.getEpic());
        }
        return associatedEpics;
    }

    /**
     * Resuelve el directorio padre y la épica asociada de una historia.
     * Los datos antiguos guardaban la épica como padre jerárquico; en ese caso
     * se recupera el directorio de la épica y se conserva la épica como asociada.
     *
     * @param historia historia a resolver
     * @param epicsById épicas del repositorio indexadas por id
     * @param associatedEpics asociaciones persistidas de historia a épica
     * @return contexto de la historia
     */
    private StoryContext resolveStoryContext(
            HistoriaUsuario historia,
            Map<Long, Epica> epicsById,
            Map<Long, Epica> associatedEpics) {

        Elemento parent = historia.getParent();
        Long parentId = parent == null ? null : parent.getId();
        String parentTitle = parent == null ? null : parent.getTitle();

        Epica legacyEpic = parentId == null ? null : epicsById.get(parentId);
        if (legacyEpic != null) {
            Elemento epicParent = legacyEpic.getParent();
            parentId = epicParent == null ? null : epicParent.getId();
            parentTitle = epicParent == null ? null : epicParent.getTitle();
        }

        Epica epic = resolveEpicForStory(historia, epicsById, associatedEpics);

        return new StoryContext(parentId, parentTitle, epic);
    }

    /**
     * Resuelve la épica de una historia: primero la asociación persistida en
     * EsAsociado y, si no existe, los datos antiguos que guardaban la épica
     * como padre jerárquico.
     *
     * @param historia historia a resolver
     * @param epicsById épicas del repositorio indexadas por id
     * @param associatedEpics asociaciones persistidas de historia a épica
     * @return épica de la historia o {@code null} si no tiene
     */
    private Epica resolveEpicForStory(
            HistoriaUsuario historia,
            Map<Long, Epica> epicsById,
            Map<Long, Epica> associatedEpics) {

        Epica associated = associatedEpics.get(historia.getId());
        if (associated != null) {
            return associated;
        }

        Elemento parent = historia.getParent();
        return parent == null ? null : epicsById.get(parent.getId());
    }

    /**
     * Construye el resumen de una épica con sus historias asociadas.
     *
     * @param epica épica del repositorio
     * @param historias historias asociadas a la épica
     * @return resumen de la épica
     */
    private EpicaResumenResponse toEpicSummary(Epica epica, List<HistoriaUsuario> historias) {
        EpicaResumenResponse response = new EpicaResumenResponse();
        response.setId(epica.getId());
        response.setTitulo(epica.getTitle());

        Elemento parent = epica.getParent();
        if (parent != null) {
            response.setIdElementoPadre(parent.getId());
            response.setTituloPadre(parent.getTitle());
        }

        List<HistoriaResumenResponse> storyResponses = historias.stream()
                .sorted(Comparator.comparing(HistoriaUsuario::getId))
                .map(this::toHistoriaResumen)
                .toList();
        response.setHistorias(storyResponses);
        response.setTotalHistorias(storyResponses.size());
        return response;
    }

    /**
     * Convierte una historia en su resumen para el panel de información.
     *
     * @param historia historia persistida
     * @return datos mínimos de la historia
     */
    private HistoriaResumenResponse toHistoriaResumen(HistoriaUsuario historia) {
        return new HistoriaResumenResponse(
                historia.getId(),
                historia.getIdentifier(),
                historia.getTitle(),
                historia.getStatus());
    }

    /**
     * Construye el contexto de una historia recién creada o modificada.
     *
     * @param historia historia persistida
     * @param epic épica asociada (puede ser {@code null})
     * @return contexto de la historia
     */
    private StoryContext contextFor(HistoriaUsuario historia, Epica epic) {
        Elemento parent = historia.getParent();
        return new StoryContext(
                parent == null ? null : parent.getId(),
                parent == null ? null : parent.getTitle(),
                epic);
    }

    /**
     * Sincroniza en EsAsociado la épica de una historia.
     * Cada historia queda asociada como máximo a una épica.
     *
     * @param historia historia a sincronizar
     * @param epic épica seleccionada o {@code null} para quitarla
     */
    private void syncStoryEpicAssociation(HistoriaUsuario historia, Epica epic) {
        List<EsAsociado> current = esAsociadoRepository.findByIdStoryId(historia.getId());

        if (epic == null) {
            current.forEach(esAsociadoRepository::delete);
            return;
        }
        if (current.size() == 1 && epic.getId().equals(current.get(0).getEpic().getId())) {
            return;
        }

        current.forEach(esAsociadoRepository::delete);
        // Se fuerza el DELETE antes de insertar la nueva asociación.
        esAsociadoRepository.flush();

        EsAsociado association = new EsAsociado();
        association.setId(new EsAsociadoId(historia.getId(), epic.getId()));
        association.setStory(historia);
        association.setEpic(epic);
        esAsociadoRepository.save(association);
    }

    /**
     * Contexto de una historia: directorio padre y épica asociada.
     */
    private record StoryContext(Long parentId, String parentTitle, Epica epic) {
    }

    /**
     * Convierte la entidad en el DTO que consume el dashboard.
     *
     * @param elemento elemento persistido
     * @return datos del elemento con su tipo
     */
    private ElementoResponse toResponse(Elemento elemento) {
        return toResponse(elemento, null);
    }

    /**
     * Convierte la entidad en el DTO y aplica el contexto de una historia.
     *
     * @param elemento elemento persistido
     * @param storyContext contexto de la historia, solo para historias
     * @return datos del elemento con su tipo
     */
    private ElementoResponse toResponse(Elemento elemento, StoryContext storyContext) {
        ElementoResponse response = new ElementoResponse();
        response.setId(elemento.getId());
        response.setTitulo(elemento.getTitle());
        response.setDescripcion(elemento.getDescription());
        response.setFechaCreacion(elemento.getCreatedAt());
        response.setFechaUltModificacion(elemento.getLastModifiedAt());
        response.setVersion(elemento.getVersion() == null ? 0L : elemento.getVersion());

        if (elemento.getParent() != null) {
            response.setIdElementoPadre(elemento.getParent().getId());
            response.setTituloPadre(elemento.getParent().getTitle());
        }

        if (elemento instanceof HistoriaUsuario historia) {
            response.setTipo("HISTORIA");
            response.setIdentificador(historia.getIdentifier());
            response.setReglasNegocio(historia.getBusinessRules());
            response.setCriteriosAceptacion(historia.getAcceptanceCriteria());
            response.setEstado(historia.getStatus());

            if (storyContext != null) {
                response.setIdElementoPadre(storyContext.parentId());
                response.setTituloPadre(storyContext.parentTitle());
                if (storyContext.epic() != null) {
                    response.setIdEpica(storyContext.epic().getId());
                    response.setTituloEpica(storyContext.epic().getTitle());
                }
            }
        } else if (elemento instanceof Epica) {
            response.setTipo("EPICA");
        } else if (elemento instanceof Directorio directorio) {
            response.setTipo("DIRECTORIO");
            response.setColor(directorio.getColor());
        } else {
            response.setTipo("ELEMENTO");
        }

        return response;
    }
}

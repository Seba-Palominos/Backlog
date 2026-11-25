package com.backlog.service.elemento;

import com.backlog.service.elemento.dto.ElementoResponse;
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
import com.backlog.service.elemento.services.ElementoService;
import com.backlog.service.proyecto.entitys.Proyecto;
import com.backlog.service.proyecto.services.ProyectoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la relación entre historias de usuario y épicas.
 * La épica se guarda como asociación (EsAsociado) y no como padre jerárquico,
 * de modo que la historia sigue apareciendo en el listado del directorio.
 */
@ExtendWith(MockitoExtension.class)
class ElementoServiceHistoriaEpicaTest {

    @Mock
    private DirectorioRepository directorioRepository;

    @Mock
    private EpicaRepository epicaRepository;

    @Mock
    private HistoriaUsuarioRepository historiaUsuarioRepository;

    @Mock
    private EsAsociadoRepository esAsociadoRepository;

    @Mock
    private ElementoRepository elementoRepository;

    @Mock
    private ProyectoService proyectoService;

    @InjectMocks
    private ElementoService elementoService;

    @Test
    void createStoryAsociaLaEpicaSinCambiarElDirectorioPadre() {
        Proyecto proyecto = proyecto(1L);
        Directorio directorio = directorio(10L, "Carpeta", proyecto);
        Epica epica = epica(20L, "Épica de facturación", proyecto);

        when(proyectoService.getAccessibleProject(1L)).thenReturn(proyecto);
        when(directorioRepository.findById(10L)).thenReturn(Optional.of(directorio));
        when(epicaRepository.findById(20L)).thenReturn(Optional.of(epica));
        when(historiaUsuarioRepository.save(any(HistoriaUsuario.class))).thenAnswer(invocation -> {
            HistoriaUsuario historia = invocation.getArgument(0);
            historia.setId(30L);
            return historia;
        });
        when(esAsociadoRepository.findByIdStoryId(30L)).thenReturn(List.of());

        HistoriaRequest request = new HistoriaRequest();
        request.setIdentificador("HU-01");
        request.setTitulo("Registrar factura");
        request.setEstado("en desarrollo");
        request.setIdElementoPadre(10L);
        request.setIdEpicaPadre(20L);

        ElementoResponse response = elementoService.createStory(1L, request);

        assertEquals(10L, response.getIdElementoPadre());
        assertEquals("Carpeta", response.getTituloPadre());
        assertEquals(20L, response.getIdEpica());
        assertEquals("Épica de facturación", response.getTituloEpica());
        verify(esAsociadoRepository).save(any(EsAsociado.class));
    }

    @Test
    void listByParentIncluyeLaHistoriaConSuEpicaAsociada() {
        Proyecto proyecto = proyecto(1L);
        Directorio directorio = directorio(10L, "Carpeta", proyecto);
        Epica epica = epica(20L, "Épica de facturación", proyecto);
        HistoriaUsuario historia = historia(30L, "Registrar factura", "aprobada", directorio, proyecto);

        EsAsociado asociacion = new EsAsociado();
        asociacion.setId(new EsAsociadoId(30L, 20L));
        asociacion.setStory(historia);
        asociacion.setEpic(epica);

        when(proyectoService.getAccessibleProject(1L)).thenReturn(proyecto);
        when(directorioRepository.findById(10L)).thenReturn(Optional.of(directorio));
        when(directorioRepository.findByProject_Id(1L)).thenReturn(List.of(directorio));
        when(epicaRepository.findByProject_Id(1L)).thenReturn(List.of(epica));
        when(historiaUsuarioRepository.findByProject_Id(1L)).thenReturn(List.of(historia));
        when(esAsociadoRepository.findByProjectId(1L)).thenReturn(List.of(asociacion));

        List<ElementoResponse> elementos = elementoService.listByParent(1L, 10L);

        ElementoResponse respuestaHistoria = elementos.stream()
                .filter(elemento -> "HISTORIA".equals(elemento.getTipo()))
                .findFirst()
                .orElse(null);

        assertNotNull(respuestaHistoria);
        assertEquals(10L, respuestaHistoria.getIdElementoPadre());
        assertEquals("aprobada", respuestaHistoria.getEstado());
        assertEquals(20L, respuestaHistoria.getIdEpica());
        assertEquals("Épica de facturación", respuestaHistoria.getTituloEpica());
    }

    @Test
    void listByParentMuestraSinEpicaCuandoLaHistoriaNoEstaAsociada() {
        Proyecto proyecto = proyecto(1L);
        Directorio directorio = directorio(10L, "Carpeta", proyecto);
        HistoriaUsuario historia = historia(30L, "Historia suelta", "en desarrollo", directorio, proyecto);

        when(proyectoService.getAccessibleProject(1L)).thenReturn(proyecto);
        when(directorioRepository.findById(10L)).thenReturn(Optional.of(directorio));
        when(directorioRepository.findByProject_Id(1L)).thenReturn(List.of(directorio));
        when(epicaRepository.findByProject_Id(1L)).thenReturn(List.of());
        when(historiaUsuarioRepository.findByProject_Id(1L)).thenReturn(List.of(historia));
        when(esAsociadoRepository.findByProjectId(1L)).thenReturn(List.of());

        ElementoResponse respuestaHistoria = elementoService.listByParent(1L, 10L).stream()
                .filter(elemento -> "HISTORIA".equals(elemento.getTipo()))
                .findFirst()
                .orElseThrow();

        assertEquals(null, respuestaHistoria.getIdEpica());
        assertEquals(null, respuestaHistoria.getTituloEpica());
    }

    @Test
    void listEpicsByProjectIncluyeEpicasDeCualquierDirectorio() {
        Proyecto proyecto = proyecto(1L);
        Directorio carpetaA = directorio(10L, "Carpeta A", proyecto);
        Directorio carpetaB = directorio(11L, "Carpeta B", proyecto);
        Epica epicaRaiz = epica(20L, "Épica raíz", proyecto);
        Epica epicaA = epica(21L, "Épica de A", proyecto);
        epicaA.setParent(carpetaA);
        Epica epicaB = epica(22L, "Épica de B", proyecto);
        epicaB.setParent(carpetaB);

        when(proyectoService.getAccessibleProject(1L)).thenReturn(proyecto);
        when(epicaRepository.findByProject_Id(1L)).thenReturn(List.of(epicaB, epicaRaiz, epicaA));

        List<ElementoResponse> epicas = elementoService.listEpicsByProject(1L);

        assertEquals(List.of(20L, 21L, 22L),
                epicas.stream().map(ElementoResponse::getId).toList());
        assertEquals("Carpeta B", epicas.get(2).getTituloPadre());
    }

    @Test
    void createStoryAsociaUnaEpicaDeOtroDirectorioDelMismoProyecto() {
        Proyecto proyecto = proyecto(1L);
        Directorio carpetaA = directorio(10L, "Carpeta A", proyecto);
        Directorio carpetaB = directorio(11L, "Carpeta B", proyecto);
        Epica epicaEnB = epica(22L, "Épica de B", proyecto);
        epicaEnB.setParent(carpetaB);

        when(proyectoService.getAccessibleProject(1L)).thenReturn(proyecto);
        when(directorioRepository.findById(10L)).thenReturn(Optional.of(carpetaA));
        when(epicaRepository.findById(22L)).thenReturn(Optional.of(epicaEnB));
        when(historiaUsuarioRepository.save(any(HistoriaUsuario.class))).thenAnswer(invocation -> {
            HistoriaUsuario historia = invocation.getArgument(0);
            historia.setId(30L);
            return historia;
        });
        when(esAsociadoRepository.findByIdStoryId(30L)).thenReturn(List.of());

        HistoriaRequest request = new HistoriaRequest();
        request.setIdentificador("HU-02");
        request.setTitulo("Historia en carpeta A");
        request.setEstado("en desarrollo");
        request.setIdElementoPadre(10L);
        request.setIdEpicaPadre(22L);

        ElementoResponse response = elementoService.createStory(1L, request);

        assertEquals(10L, response.getIdElementoPadre());
        assertEquals("Carpeta A", response.getTituloPadre());
        assertEquals(22L, response.getIdEpica());
        assertEquals("Épica de B", response.getTituloEpica());
        verify(esAsociadoRepository).save(any(EsAsociado.class));
    }

    @Test
    void listEpicSummaryAgrupaLasHistoriasAsociadasDeTodoElProyecto() {
        Proyecto proyecto = proyecto(1L);
        Directorio carpetaA = directorio(10L, "Carpeta A", proyecto);
        Directorio carpetaB = directorio(11L, "Carpeta B", proyecto);
        Epica epicaEnB = epica(22L, "Épica de B", proyecto);
        epicaEnB.setParent(carpetaB);
        Epica epicaSinHistorias = epica(23L, "Épica vacía", proyecto);

        HistoriaUsuario asociada = historia(30L, "Historia asociada", "aprobada", carpetaA, proyecto);
        asociada.setIdentifier("US-10");
        HistoriaUsuario heredada = historia(31L, "Historia heredada", "rechazada", epicaEnB, proyecto);
        heredada.setIdentifier("US-11");
        HistoriaUsuario sinEpica = historia(32L, "Historia suelta", "en desarrollo", carpetaA, proyecto);

        EsAsociado asociacion = new EsAsociado();
        asociacion.setId(new EsAsociadoId(30L, 22L));
        asociacion.setStory(asociada);
        asociacion.setEpic(epicaEnB);

        when(proyectoService.getAccessibleProject(1L)).thenReturn(proyecto);
        when(epicaRepository.findByProject_Id(1L)).thenReturn(List.of(epicaSinHistorias, epicaEnB));
        when(historiaUsuarioRepository.findByProject_Id(1L))
                .thenReturn(List.of(asociada, heredada, sinEpica));
        when(esAsociadoRepository.findByProjectId(1L)).thenReturn(List.of(asociacion));

        List<EpicaResumenResponse> resumen = elementoService.listEpicSummary(1L);

        assertEquals(2, resumen.size());
        assertEquals(22L, resumen.get(0).getId());
        assertEquals("Carpeta B", resumen.get(0).getTituloPadre());
        assertEquals(2, resumen.get(0).getTotalHistorias());
        assertEquals(List.of("US-10", "US-11"),
                resumen.get(0).getHistorias().stream()
                        .map(HistoriaResumenResponse::getIdentificador)
                        .toList());
        assertEquals(23L, resumen.get(1).getId());
        assertEquals(0, resumen.get(1).getTotalHistorias());
    }

    private Proyecto proyecto(Long id) {
        Proyecto proyecto = new Proyecto();
        proyecto.setId(id);
        return proyecto;
    }

    private Directorio directorio(Long id, String titulo, Proyecto proyecto) {
        Directorio directorio = new Directorio();
        directorio.setId(id);
        directorio.setTitle(titulo);
        directorio.setProject(proyecto);
        return directorio;
    }

    private Epica epica(Long id, String titulo, Proyecto proyecto) {
        Epica epica = new Epica();
        epica.setId(id);
        epica.setTitle(titulo);
        epica.setProject(proyecto);
        return epica;
    }

    private HistoriaUsuario historia(Long id, String titulo, String estado, Elemento padre,
            Proyecto proyecto) {
        HistoriaUsuario historia = new HistoriaUsuario();
        historia.setId(id);
        historia.setTitle(titulo);
        historia.setStatus(estado);
        historia.setParent(padre);
        historia.setProject(proyecto);
        return historia;
    }
}

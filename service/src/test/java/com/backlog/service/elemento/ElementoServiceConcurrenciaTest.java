package com.backlog.service.elemento;

import com.backlog.service.elemento.dto.ElementoResponse;
import com.backlog.service.elemento.dto.ElementoUpdateRequest;
import com.backlog.service.elemento.entitys.Directorio;
import com.backlog.service.elemento.repository.DirectorioRepository;
import com.backlog.service.elemento.repository.ElementoRepository;
import com.backlog.service.elemento.repository.EsAsociadoRepository;
import com.backlog.service.elemento.repository.EpicaRepository;
import com.backlog.service.elemento.repository.HistoriaUsuarioRepository;
import com.backlog.service.elemento.services.ElementoService;
import com.backlog.service.exception.ConflictoConcurrenciaException;
import com.backlog.service.proyecto.entitys.Proyecto;
import com.backlog.service.proyecto.services.ProyectoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del control de concurrencia optimista al modificar elementos.
 */
@ExtendWith(MockitoExtension.class)
class ElementoServiceConcurrenciaTest {

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
    void updateElementRechazaVersionDesactualizada() {
        Proyecto proyecto = new Proyecto();
        proyecto.setId(1L);
        Directorio directorio = new Directorio();
        directorio.setId(10L);
        directorio.setTitle("Directorio");
        directorio.setProject(proyecto);
        directorio.setVersion(3L);

        when(proyectoService.getAccessibleProject(1L)).thenReturn(proyecto);
        when(directorioRepository.findById(10L)).thenReturn(Optional.of(directorio));

        ElementoUpdateRequest request = new ElementoUpdateRequest();
        request.setTitulo("Directorio editado");
        request.setVersion(2L);

        assertThrows(ConflictoConcurrenciaException.class,
                () -> elementoService.updateElement(1L, 10L, request));
    }

    @Test
    void updateElementAceptaVersionVigente() {
        Proyecto proyecto = new Proyecto();
        proyecto.setId(1L);
        Directorio directorio = new Directorio();
        directorio.setId(10L);
        directorio.setTitle("Directorio");
        directorio.setProject(proyecto);
        directorio.setVersion(3L);

        when(proyectoService.getAccessibleProject(1L)).thenReturn(proyecto);
        when(directorioRepository.findById(10L)).thenReturn(Optional.of(directorio));
        when(elementoRepository.save(directorio)).thenReturn(directorio);

        ElementoUpdateRequest request = new ElementoUpdateRequest();
        request.setTitulo("Directorio editado");
        request.setVersion(3L);

        ElementoResponse response = elementoService.updateElement(1L, 10L, request);

        assertEquals(3L, response.getVersion());
        assertEquals("Directorio editado", response.getTitulo());
        verify(elementoRepository).save(directorio);
    }
}

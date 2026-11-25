package com.backlog.service.elemento.composite;

import com.backlog.service.elemento.entitys.Elemento;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de la propagación recursiva del mensaje de borrado en el Composite.
 */
class DirectorioCompositeTest {

    @Test
    void deletePropagaElMensajeAHijosAntesQueAlDirectorio() {
        List<String> borrados = new ArrayList<>();
        Elemento directorio = new Elemento();
        directorio.setId(1L);

        DirectorioComposite root = new DirectorioComposite(null, elemento -> borrados.add("raiz"));
        root.add(new HojaElemento(new Elemento(), elemento -> borrados.add("hoja")));

        DirectorioComposite hijo = new DirectorioComposite(
                directorio, elemento -> borrados.add("directorio"));
        hijo.add(new HojaElemento(new Elemento(), elemento -> borrados.add("nieto")));
        root.add(hijo);

        root.delete();

        assertEquals(List.of("hoja", "nieto", "directorio"), borrados);
    }
}

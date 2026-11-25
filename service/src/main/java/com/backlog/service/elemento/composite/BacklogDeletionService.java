package com.backlog.service.elemento.composite;

import com.backlog.service.autenticacion.repository.TienePermisoRepository;
import com.backlog.service.elemento.entitys.Elemento;
import com.backlog.service.elemento.entitys.Epica;
import com.backlog.service.elemento.entitys.HistoriaUsuario;
import com.backlog.service.elemento.repository.ElementoRepository;
import com.backlog.service.elemento.repository.EsAsociadoRepository;
import com.backlog.service.proyecto.entitys.Proyecto;
import com.backlog.service.proyecto.repository.ParticipaRepository;
import com.backlog.service.proyecto.repository.ProyectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio que arma el árbol Composite del backlog y propaga el borrado recursivo
 * a los hijos de un elemento o a todos los elementos de un proyecto.
 */
@Component
public class BacklogDeletionService {

    @Autowired
    private ElementoRepository elementoRepository;

    @Autowired
    private EsAsociadoRepository esAsociadoRepository;

    @Autowired
    private ParticipaRepository participaRepository;

    @Autowired
    private TienePermisoRepository tienePermisoRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    /**
     * Borra un elemento y toda su descendencia enviando el mensaje a cada hijo.
     *
     * @param elemento elemento que se quiere eliminar
     */
    @Transactional
    public void deleteElementTree(Elemento elemento) {
        buildComponent(elemento).delete();
    }

    /**
     * Borra todos los elementos de un proyecto y después el proyecto, sus participaciones
     * y sus permisos asignados.
     *
     * @param proyecto proyecto que se quiere eliminar
     */
    @Transactional
    public void deleteProjectTree(Proyecto proyecto) {
        DirectorioComposite root = new DirectorioComposite(null, this::deleteSingleElement);
        elementoRepository.findByProject_IdAndParentIsNull(proyecto.getId())
                .forEach(elemento -> root.add(buildComponent(elemento)));
        root.delete();

        participaRepository.findByIdProjectId(proyecto.getId())
                .forEach(participaRepository::delete);
        tienePermisoRepository.findByIdProjectId(proyecto.getId())
                .forEach(tienePermisoRepository::delete);
        proyectoRepository.delete(proyecto);
    }

    /**
     * Construye el componente correspondiente, tratando como compuesto cualquier elemento
     * con hijos y como hoja los que no los tienen.
     *
     * @param elemento elemento desde el que se arma el árbol
     * @return componente del Composite
     */
    private BacklogComponent buildComponent(Elemento elemento) {
        List<Elemento> children = elementoRepository.findByParent_Id(elemento.getId());
        if (children.isEmpty()) {
            return new HojaElemento(elemento, this::deleteSingleElement);
        }

        DirectorioComposite composite = new DirectorioComposite(elemento, this::deleteSingleElement);
        children.forEach(child -> composite.add(buildComponent(child)));
        return composite;
    }

    /**
     * Elimina una hoja o un directorio ya vacío, limpiando antes sus asociaciones.
     *
     * @param elemento elemento a eliminar
     */
    private void deleteSingleElement(Elemento elemento) {
        if (elemento instanceof HistoriaUsuario historia) {
            esAsociadoRepository.findByIdStoryId(historia.getId())
                    .forEach(esAsociadoRepository::delete);
        } else if (elemento instanceof Epica epica) {
            esAsociadoRepository.findByIdEpicId(epica.getId())
                    .forEach(esAsociadoRepository::delete);
        }
        elementoRepository.delete(elemento);
    }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ActualizarElementoRequest,
  CrearDirectorioRequest,
  CrearEpicaRequest,
  CrearHistoriaRequest,
  Elemento,
  EpicaResumen,
} from '../models/elemento.model';

/**
 * Servicio que consume los endpoints de elementos del backlog.
 */
@Injectable({ providedIn: 'root' })
export class ElementoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/proyectos`;

  /** Lista los elementos de un repositorio o los hijos directos de un directorio. */
  listarPorProyecto(projectId: number, parentId?: number | null): Observable<Elemento[]> {
    return this.http.get<Elemento[]>(`${this.baseUrl}/${projectId}/elementos`, {
      params: parentId != null ? { parentId } : undefined,
    });
  }

  /** Lista todas las épicas del repositorio, sin importar su directorio. */
  listarEpicas(projectId: number): Observable<Elemento[]> {
    return this.http.get<Elemento[]>(`${this.baseUrl}/${projectId}/epicas`);
  }

  /** Lista el resumen de las épicas con sus historias asociadas. */
  listarResumenEpicas(projectId: number): Observable<EpicaResumen[]> {
    return this.http.get<EpicaResumen[]>(`${this.baseUrl}/${projectId}/epicas/resumen`);
  }

  /** Crea un directorio dentro del repositorio. */
  crearDirectorio(projectId: number, request: CrearDirectorioRequest): Observable<Elemento> {
    return this.http.post<Elemento>(`${this.baseUrl}/${projectId}/directorios`, request);
  }

  /** Obtiene el detalle de un elemento del repositorio. */
  obtenerElemento(projectId: number, elementId: number): Observable<Elemento> {
    return this.http.get<Elemento>(`${this.baseUrl}/${projectId}/elementos/${elementId}`);
  }

  /** Modifica un elemento del repositorio. */
  actualizarElemento(
    projectId: number,
    elementId: number,
    request: ActualizarElementoRequest,
  ): Observable<Elemento> {
    return this.http.put<Elemento>(
      `${this.baseUrl}/${projectId}/elementos/${elementId}`,
      request,
    );
  }

  /** Elimina un elemento del repositorio. */
  eliminarElemento(projectId: number, elementId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}/elementos/${elementId}`);
  }

  /** Crea una épica dentro del repositorio. */
  crearEpica(projectId: number, request: CrearEpicaRequest): Observable<Elemento> {
    return this.http.post<Elemento>(`${this.baseUrl}/${projectId}/epicas`, request);
  }

  /** Crea una historia de usuario dentro del repositorio. */
  crearHistoria(projectId: number, request: CrearHistoriaRequest): Observable<Elemento> {
    return this.http.post<Elemento>(`${this.baseUrl}/${projectId}/historias`, request);
  }
}

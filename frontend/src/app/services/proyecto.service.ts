import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CrearProyectoRequest, Proyecto } from '../models/proyecto.model';

/**
 * Servicio que consume los endpoints de repositorios (proyectos) del backend.
 */
@Injectable({ providedIn: 'root' })
export class ProyectoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/proyectos`;

  /** Lista los repositorios creados por el usuario autenticado. */
  listarPropios(): Observable<Proyecto[]> {
    return this.http.get<Proyecto[]>(this.baseUrl);
  }

  /** Lista los repositorios compartidos con el usuario autenticado. */
  listarCompartidos(): Observable<Proyecto[]> {
    return this.http.get<Proyecto[]>(`${this.baseUrl}/compartidos`);
  }

  /** Obtiene un repositorio accesible para el usuario autenticado. */
  obtener(projectId: number): Observable<Proyecto> {
    return this.http.get<Proyecto>(`${this.baseUrl}/${projectId}`);
  }

  /** Crea un repositorio para el usuario autenticado. */
  crear(request: CrearProyectoRequest): Observable<Proyecto> {
    return this.http.post<Proyecto>(this.baseUrl, request);
  }

  /**
   * Elimina un repositorio y todos sus elementos de forma recursiva.
   * Solo el propietario puede invocar esta operación.
   */
  eliminar(projectId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}`);
  }
}

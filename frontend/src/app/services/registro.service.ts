import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { RegistroRequest } from '../models/registro-request.model';

/**
 * Servicio que consume los endpoints de registro del backend.
 */
@Injectable({ providedIn: 'root' })
export class RegistroService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/registro`;

  /**
   * Envía los datos del formulario al endpoint POST /api/registro.
   *
   * @param request datos del nuevo usuario
   * @returns observable que se completa con la respuesta 201 del backend
   */
  registrar(request: RegistroRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}`, request);
  }
}

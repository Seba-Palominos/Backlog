import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginRequest } from '../models/login-request.model';
import { LoginResponse } from '../models/login-response.model';

/**
 * Servicio que consume el endpoint de autenticación del backend.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  /**
   * Envía las credenciales al endpoint POST /api/auth/login.
   *
   * @param request credenciales del usuario
   * @returns observable con el token JWT generado
   */
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, request);
  }

  /**
   * Envía el ID token de Google al endpoint POST /api/auth/google.
   *
   * @param credential ID token entregado por Google Identity Services
   * @returns observable con el token JWT del servicio y el nombre de usuario
   */
  googleLogin(credential: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/google`, { credential });
  }
}

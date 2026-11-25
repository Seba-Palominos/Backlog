import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { SessionService } from '../services/session.service';

/**
 * Interceptor que agrega el token JWT a las peticiones de la API del backlog.
 * El token se obtiene del servicio de sesión, que valida su expiración.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(SessionService).getToken();

  if (!token || !request.url.startsWith(environment.apiUrl)) {
    return next(request);
  }

  const authenticatedRequest = request.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });

  return next(authenticatedRequest);
};

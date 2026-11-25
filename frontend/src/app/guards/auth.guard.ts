import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from '../services/session.service';

/**
 * Guard que impide abrir el dashboard sin una sesión activa.
 * Si no hay sesión, redirige al login conservando la ruta solicitada.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.hasActiveSession()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url },
  });
};

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from '../services/session.service';

/**
 * Guard que evita que un usuario con sesión activa vuelva al login o al registro.
 */
export const guestGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  return session.hasActiveSession() ? router.createUrlTree(['/dashboard']) : true;
};

import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { SessionService } from '../services/session.service';
import { authGuard } from './auth.guard';

const route = {} as ActivatedRouteSnapshot;
const state = { url: '/dashboard' } as RouterStateSnapshot;

describe('authGuard', () => {
  function configure(activeSession: boolean): Router {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: SessionService,
          useValue: { hasActiveSession: () => activeSession },
        },
      ],
    });

    return TestBed.inject(Router);
  }

  it('permite el acceso cuando hay una sesión activa', () => {
    configure(true);

    const result = TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(result).toBe(true);
  });

  it('redirige al login conservando la ruta solicitada cuando no hay sesión', () => {
    const router = configure(false);

    const result = TestBed.runInInjectionContext(() => authGuard(route, state)) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/login?returnUrl=%2Fdashboard');
  });
});

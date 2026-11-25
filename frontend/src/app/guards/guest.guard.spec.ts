import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { SessionService } from '../services/session.service';
import { guestGuard } from './guest.guard';

const route = {} as ActivatedRouteSnapshot;
const state = { url: '/login' } as RouterStateSnapshot;

describe('guestGuard', () => {
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

  it('redirige al dashboard cuando ya hay una sesión activa', () => {
    const router = configure(true);

    const result = TestBed.runInInjectionContext(() => guestGuard(route, state)) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/dashboard');
  });

  it('permite el acceso cuando no hay sesión', () => {
    configure(false);

    const result = TestBed.runInInjectionContext(() => guestGuard(route, state));

    expect(result).toBe(true);
  });
});

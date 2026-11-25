import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { guestGuard } from './guards/guest.guard';

/**
 * Rutas de la aplicación.
 * La página de registro es la vista inicial; el acceso y el dashboard tienen rutas propias.
 */
export const routes: Routes = [
  { path: '', redirectTo: 'registro', pathMatch: 'full' },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login').then((component) => component.Login),
  },
  {
    path: 'registro',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./pages/registro/registro').then((component) => component.Registro),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/dashboard/dashboard').then((component) => component.Dashboard),
  },
  {
    path: 'repositorios/:id/epicas/nueva',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/epica-plantilla/epica-plantilla').then(
        (component) => component.EpicaPlantilla,
      ),
  },
  {
    path: 'repositorios/:id/historias/nueva',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/historia-plantilla/historia-plantilla').then(
        (component) => component.HistoriaPlantilla,
      ),
  },
  {
    path: 'repositorios/:id/historias/:historiaId/editar',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/historia-plantilla/historia-plantilla').then(
        (component) => component.HistoriaPlantilla,
      ),
  },
  {
    path: 'repositorios/:id/directorios/:directorioId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/repositorio/repositorio').then((component) => component.Repositorio),
  },
  {
    path: 'repositorios/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/repositorio/repositorio').then((component) => component.Repositorio),
  },
  // Cualquier ruta aún no implementada vuelve al registro.
  { path: '**', redirectTo: 'registro' },
];

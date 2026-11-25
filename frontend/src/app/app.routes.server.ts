import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    // El dashboard depende de la sesión del navegador; se renderiza solo en el cliente.
    path: 'dashboard',
    renderMode: RenderMode.Client,
  },
  {
    // El detalle del repositorio y sus plantillas también dependen de la sesión.
    path: 'repositorios/**',
    renderMode: RenderMode.Client,
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender,
  },
];

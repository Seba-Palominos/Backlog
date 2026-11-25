import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { ProyectoService } from './proyecto.service';

/**
 * Pruebas de las llamadas HTTP del servicio de repositorios.
 */
describe('ProyectoService', () => {
  let service: ProyectoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProyectoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('elimina un repositorio con DELETE y sin cuerpo', () => {
    let completed = false;

    service.eliminar(42).subscribe({
      next: () => undefined,
      complete: () => (completed = true),
    });

    const request = http.expectOne(`${environment.apiUrl}/proyectos/42`);
    expect(request.request.method).toBe('DELETE');
    expect(request.request.body).toBeNull();

    request.flush(null, { status: 204, statusText: 'No Content' });
    expect(completed).toBe(true);
  });
});

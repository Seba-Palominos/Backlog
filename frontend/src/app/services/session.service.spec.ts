import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { SessionService } from './session.service';

function createToken(payload: Record<string, unknown>): string {
  const encode = (value: Record<string, unknown>): string =>
    btoa(JSON.stringify(value))
      .replace(/=/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');

  return `${encode({ alg: 'none', typ: 'JWT' })}.${encode(payload)}.signature`;
}

describe('SessionService', () => {
  let service: SessionService;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [{ provide: PLATFORM_ID, useValue: 'browser' }],
    });
    service = TestBed.inject(SessionService);
  });

  it('guarda y recupera una sesión vigente', () => {
    const token = createToken({ exp: Math.floor(Date.now() / 1000) + 60 });

    service.save(token, 'ana', true);

    expect(service.hasActiveSession()).toBe(true);
    expect(service.getToken()).toBe(token);
    expect(service.getUsername()).toBe('ana');
  });

  it('descarta un token expirado y limpia el almacenamiento', () => {
    const token = createToken({ exp: Math.floor(Date.now() / 1000) - 60 });

    service.save(token, 'ana', false);

    expect(service.hasActiveSession()).toBe(false);
    expect(localStorage.getItem('authToken')).toBeNull();
    expect(sessionStorage.getItem('authToken')).toBeNull();
  });

  it('trata un token malformado como sesión inválida', () => {
    service.save('token-invalido', 'ana', true);

    expect(service.hasActiveSession()).toBe(false);
  });

  it('elimina la sesión al cerrar sesión', () => {
    const token = createToken({ exp: Math.floor(Date.now() / 1000) + 60 });

    service.save(token, 'ana', true);
    service.clear();

    expect(service.hasActiveSession()).toBe(false);
    expect(service.getUsername()).toBe('');
  });
});

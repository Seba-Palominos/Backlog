import { isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';

const TOKEN_KEY = 'authToken';
const USERNAME_KEY = 'authUsername';

/**
 * Centraliza el almacenamiento y la validación de la sesión del usuario.
 * El token se guarda en localStorage o sessionStorage según "Recordar esta sesión".
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly platformId = inject(PLATFORM_ID);

  /** Guarda el token y el nombre de usuario en el almacenamiento elegido. */
  save(token: string, username: string, remember: boolean): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    try {
      this.clear();
      const storage = remember ? localStorage : sessionStorage;
      storage.setItem(TOKEN_KEY, token);
      storage.setItem(USERNAME_KEY, username);
    } catch {
      // Si el navegador bloquea el almacenamiento, la sesión no persiste.
    }
  }

  /** Devuelve el token si la sesión existe y no ha expirado. */
  getToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }

    try {
      const token = localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY);
      if (!token) {
        return null;
      }

      if (this.isExpired(token)) {
        this.clear();
        return null;
      }

      return token;
    } catch {
      return null;
    }
  }

  /** Devuelve el nombre del usuario autenticado. */
  getUsername(): string {
    if (!isPlatformBrowser(this.platformId)) {
      return '';
    }

    try {
      return localStorage.getItem(USERNAME_KEY) ?? sessionStorage.getItem(USERNAME_KEY) ?? '';
    } catch {
      return '';
    }
  }

  /** Indica si hay una sesión válida. */
  hasActiveSession(): boolean {
    return this.getToken() !== null;
  }

  /** Elimina la sesión de ambos almacenamientos. */
  clear(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USERNAME_KEY);
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USERNAME_KEY);
    } catch {
      // Sin almacenamiento disponible no hay nada que limpiar.
    }
  }

  /** Comprueba la expiración del JWT sin validar la firma. */
  private isExpired(token: string): boolean {
    const payload = this.decodePayload(token);
    if (!payload) {
      return true;
    }

    if (typeof payload.exp !== 'number') {
      return false;
    }

    return payload.exp * 1000 <= Date.now();
  }

  /** Decodifica el payload del JWT para leer la fecha de expiración. */
  private decodePayload(token: string): { exp?: number } | null {
    try {
      const payload = token.split('.')[1];
      if (!payload) {
        return null;
      }

      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
      const decoded = JSON.parse(atob(padded)) as { exp?: unknown };
      return typeof decoded.exp === 'number' ? { exp: decoded.exp } : {};
    } catch {
      return null;
    }
  }
}

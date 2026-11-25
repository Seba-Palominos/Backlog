import { Injectable } from '@angular/core';
import {
  GoogleAccountsId,
  GoogleButtonOptions,
  GoogleCredentialResponse,
} from '../models/google-identity.model';

const SCRIPT_ID = 'google-identity-script';
const SCRIPT_URL = 'https://accounts.google.com/gsi/client';

/**
 * Carga y expone Google Identity Services.
 * El script se solicita solo en el navegador para no romper el renderizado en SSR.
 */
@Injectable({ providedIn: 'root' })
export class GoogleIdentityService {
  private loadPromise?: Promise<GoogleAccountsId>;

  /** Indica si el código corre en el navegador y no en el servidor. */
  isBrowser(): boolean {
    return typeof window !== 'undefined' && typeof document !== 'undefined';
  }

  /** Carga el script de Google una sola vez y devuelve la API de cuentas. */
  load(): Promise<GoogleAccountsId> {
    if (!this.isBrowser()) {
      return Promise.reject(
        new Error('Google Identity Services solo está disponible en el navegador'),
      );
    }

    const loaded = window.google?.accounts?.id;
    if (loaded) {
      return Promise.resolve(loaded);
    }

    if (!this.loadPromise) {
      this.loadPromise = new Promise<GoogleAccountsId>((resolve, reject) => {
        const existing = document.getElementById(SCRIPT_ID);
        if (existing) {
          existing.addEventListener('load', () => this.resolveAccounts(resolve, reject));
          existing.addEventListener('error', () =>
            reject(new Error('No se pudo cargar Google Identity Services')),
          );
          return;
        }

        const script = document.createElement('script');
        script.id = SCRIPT_ID;
        script.src = SCRIPT_URL;
        script.async = true;
        script.defer = true;
        script.addEventListener('load', () => this.resolveAccounts(resolve, reject));
        script.addEventListener('error', () =>
          reject(new Error('No se pudo cargar Google Identity Services')),
        );
        document.head.appendChild(script);
      });

      // Si la carga falla, se permite reintentar en una nueva llamada.
      this.loadPromise.catch(() => {
        this.loadPromise = undefined;
      });
    }

    return this.loadPromise;
  }

  /** Inicializa Google con el Client ID y el callback de credenciales. */
  initialize(
    clientId: string,
    onCredential: (response: GoogleCredentialResponse) => void,
  ): Promise<void> {
    return this.load().then((accounts) => {
      accounts.initialize({
        client_id: clientId,
        callback: onCredential,
        auto_select: false,
        cancel_on_tap_outside: true,
      });
    });
  }

  /** Dibuja el botón oficial de Google dentro del contenedor indicado. */
  renderButton(parent: HTMLElement, options?: GoogleButtonOptions): Promise<void> {
    return this.load().then((accounts) => {
      accounts.renderButton(parent, {
        theme: 'outline',
        size: 'large',
        shape: 'rectangular',
        text: 'continue_with',
        logo_alignment: 'left',
        width: 336,
        ...options,
      });
    });
  }

  /** Evita que Google reautentique automáticamente después de cerrar sesión. */
  disableAutoSelect(): void {
    if (this.isBrowser()) {
      window.google?.accounts?.id.disableAutoSelect();
    }
  }

  /** Resuelve la promesa con la API de Google o falla con un mensaje claro. */
  private resolveAccounts(
    resolve: (accounts: GoogleAccountsId) => void,
    reject: (error: Error) => void,
  ): void {
    const accounts = window.google?.accounts?.id;
    if (accounts) {
      resolve(accounts);
    } else {
      reject(new Error('Google Identity Services no se inicializó correctamente'));
    }
  }
}

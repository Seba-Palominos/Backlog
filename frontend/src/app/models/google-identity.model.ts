/**
 * Tipos de Google Identity Services usados por el inicio de sesión.
 * Se declaran localmente para no agregar una dependencia adicional al frontend.
 * Referencia: https://developers.google.com/identity/gsi/web/reference/js-reference
 */
export interface GoogleCredentialResponse {
  credential: string;
  select_by?: string;
}

export interface GoogleButtonOptions {
  theme?: 'outline' | 'filled_blue' | 'filled_black';
  size?: 'large' | 'medium' | 'small';
  shape?: 'rectangular' | 'pill' | 'circle' | 'square';
  text?: 'signin_with' | 'signup_with' | 'continue_with' | 'signin';
  logo_alignment?: 'left' | 'center';
  width?: number;
}

export interface GoogleIdentityConfig {
  client_id: string;
  callback: (response: GoogleCredentialResponse) => void;
  auto_select?: boolean;
  cancel_on_tap_outside?: boolean;
}

export interface GoogleAccountsId {
  initialize(config: GoogleIdentityConfig): void;
  renderButton(parent: HTMLElement, options: GoogleButtonOptions): void;
  disableAutoSelect(): void;
}

declare global {
  interface Window {
    google?: {
      accounts: {
        id: GoogleAccountsId;
      };
    };
  }
}

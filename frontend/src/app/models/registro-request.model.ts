/**
 * Datos enviados al endpoint de registro.
 * Los nombres coinciden con el DTO RegistroRequest del backend.
 */
export interface RegistroRequest {
  username: string;
  email: string;
  password: string;
  password_confirm: string;
}

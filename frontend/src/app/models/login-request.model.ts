/**
 * Datos enviados al endpoint de inicio de sesión.
 * Los nombres coinciden con el DTO LoginRequest del backend.
 */
export interface LoginRequest {
  username: string;
  password: string;
}

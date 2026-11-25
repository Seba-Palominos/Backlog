/**
 * Respuesta del endpoint de inicio de sesión con el token JWT generado
 * y el nombre de usuario autenticado.
 */
export interface LoginResponse {
  token: string;
  username: string;
}

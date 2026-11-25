/**
 * Configuración de entorno para el frontend.
 * El backend Spring Boot expone la API en el puerto 8081.
 * googleClientId es el Client ID (tipo Web) de Google Cloud Console y debe
 * coincidir con security.google.client-id del backend.
 */
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api',
  googleClientId: '641279425498-ji3o0oe4qu3fhadgbm5pk9rim73d8k0i.apps.googleusercontent.com',
};

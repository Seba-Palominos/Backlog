---
name: crear-pagina-angular
description: Usar cuando se pida crear o modificar una pagina o componente en Angular dentro de frontend/.
---

# Objetivo
Implementar páginas y componentes en Angular dentro de `frontend/` con UI-UX limpia, poca carga visual y estilo básico, integrados con el backend en `http://localhost:8081`.

## Pasos obligatorios
1. **Modularizar y respetar estructura:** Separar archivos por responsabilidad (`components/`, `services/`, `models/`, `pages/`) dentro de `frontend/src/app/`, evitando componentes monolíticos o estilos globales innecesarios.
2. **Validación de formularios:** Marcar como requeridos los campos obligatorios (salvo que se indiquen como opcionales), bloquear o no enviar el formulario si es inválido y limpiar/omitir valores vacíos o `null` antes de enviar al servidor.
3. **Seguridad de rutas:** Configurar y respetar guards de ruta (`CanActivate`) para que solo se acceda a vistas autorizadas según el estado de sesión/permisos.
4. **Sesión y token:** Verificar vigencia/expiración del token y manejar respuestas `401` o sesión vencida mediante interceptor/servicio de autenticación, redirigiendo al login y limpiando la sesión cuando corresponda.
5. **Notificaciones toast:** Mostrar notificaciones toast claras ante respuestas exitosas o errores devueltos por el servidor.
6. **Verificación:** Ejecutar los tests unitarios correspondientes y comprobar que el proyecto compila sin errores antes de dar la tarea por cerrada.

## Restricciones
- No inventar endpoints ni campos que no coincidan con el backend o los modelos existentes.
- Mantener la interfaz simple, legible y con poca carga visual.
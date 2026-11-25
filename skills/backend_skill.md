---
name: crear-endpoint-spring
description: Usar cuando se pida crear o modificar un endpoint REST, controlador, servicio o DTO en el backend Spring Boot.
---
# Objetivo
Implementar endpoints RESTful siguiendo la arquitectura en capas y los estándares de `criterios.md`.

## Pasos obligatorios
1. **DTOs:** Crear records o clases separadas para Request y Response (nunca exponer entidades JPA directamente).
2. **Validación:** Agregar anotaciones de `jakarta.validation` (`@NotNull`, `@NotBlank`, `@Valid`).
3. **Seguridad / Proxy:** cuando llegue la peticion para modificar un elemento, se debe verificar que permisos tiene el usuario con respecto al proyecto.
4. **Manejo de errores:** Lanzar excepciones de dominio capturadas por el `GlobalExceptionHandler`.
5. **Verificación:** Compilar con `./mvnw test-compile` y comprobar que no haya errores de tipos.
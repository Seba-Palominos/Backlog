# Criterios y Reglas del Proyecto

## Paradigma y Arquitectura
- Programación orientada a objetos y manejo de concurrencia para múltiples usuarios simultáneos.
- Aplicar el patrón **Proxy** para controlar permisos y seguridad en las modificaciones de elementos (`Elemento`) de un proyecto (`Proyecto`).
- Aplicar el patrón **Composite** para la jerarquía y operaciones sobre directorios y elementos del backlog[cite: 1].
- Respetar la concurrencia sin alterar el modelo de base de datos diseñado[cite: 1].

## Reglas de Código y Base de Datos
- **No modificar** el modelo de base de datos relacional diseñado (entidades, relaciones ni atributos) definidos en **Backlog/docs/base-de-datos/bd.md**.
- Escribir todos los **comentarios del código en español**.
- Escribir todos los **nombres de clases, interfaces, métodos y variables en inglés**.
- Validar entradas y manejar excepciones de forma explícita.

## Reglas sobre Historias de Usuario
Toda historia de usuario debe seguir exactamente esta plantilla:

```text
ID: [identificador]
Título: [título breve de la historia de usuario]
Descripción: Como [rol], quiero [requerimiento], para [objetivo/beneficio].
Reglas de negocio:
  - [regla 1]
  - [regla 2]
Criterios de aceptación:
  - Dado [contexto inicial], cuando [acción o evento], entonces [resultado esperado].
```
## Reglas de Frontend y API
- Los endpoints, métodos HTTP y estructuras de datos (DTOs) de los controladores del backend deben cubrir las acciones y datos requeridos por los diseños ubicados en `Backlog/UI-UX/`.
- No inventar campos ni endpoints que no correspondan al dominio ni a las vistas definidas en `Backlog/UI-UX/`.
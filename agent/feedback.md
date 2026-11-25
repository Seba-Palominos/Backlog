# Feedback y Decisiones

Registro de correcciones, ajustes y decisiones tomadas sobre la marcha.
Regla de precedencia: si algo aquí matiza o corrige una propuesta anterior, prevalece lo más reciente.
Este archivo se actualiza automáticamente (sin aprobación previa) cada vez que el usuario responde una duda, da una corrección o surge una idea nueva.

## Estándares
- `agent/criterios.md` es el estándar rector del proyecto; todo el desarrollo debe respetarlo.
- No modificar el modelo relacional definido en `docs/base-de-datos/bd.md`.
- Comentarios de código en español; nombres de clases/interfaces/métodos/variables en inglés.
- No inventar campos ni endpoints fuera del dominio ni de las vistas de `docs/UI-UX`.

## Decisiones de alcance
- **Chat:** se persistirá en una base de datos **no relacional** separada. Queda fuera del modelo relacional de `bd.md`.
- **Campos no presentes en `bd.md`** (story points, sprint, metodología, visibilidad, hipótesis, métricas/OKR, estado de roadmap, responsable, color de carpeta, etc.): se obtienen **procesando los datos disponibles**, sin agregarlos al modelo relacional.

## Estructura del servicio
- El proyecto real es la carpeta `service/` (no `backlog-service/`), paquete base `com.backlog.service`.
- Los subpaquetes deben seguir la estructura definida en `agent/context.md`: `config`, `security`, `registro`, `autenticacion`, `proyecto`, `elemento` (con `services/proxy` y `composite`) y `exception`, cada uno con sus `controller`, `services`, `entitys`, `dto` y `repository` cuando aplique.

## Stack
- Backend: Java 21 + Spring Boot.
- Frontend: Angular (última versión).
- Agente IA: scripts Python con Ollama en local.

## Seguridad (implementado)
- Spring Security en modo **stateless** (sin sesiones), CSRF deshabilitado, API protegida por JWT.
- Dependencias JWT: `jjwt-api`, `jjwt-impl` y `jjwt-jackson` (versión 0.12.6), añadidas al `pom.xml`.
- Hash de contraseñas: `CustomPasswordEncoder` (BCrypt, fuerza 12) expuesto como `PasswordEncoder`.
- Manejo de tokens: `JwtService` (generar/validar), `JwtAuthenticationFilter` (lee `Authorization: Bearer`), `CurrentUserProvider` (usuario autenticado actual).
- Endpoints públicos permitidos: `/api/auth/**` y `/api/registro/**`; el resto requiere JWT.
- Propiedades: `security.jwt.secret` (vía `JWT_SECRET`) y `security.jwt.expiration` (vía `JWT_EXPIRATION`, 24 h por defecto).

## Entidades JPA (implementado)
- Mapeadas fielmente a `docs/base-de-datos/bd.md`, con `@Table`/`@Column` usando los nombres exactos del modelo.
- `Usuario` (registro) y `Permiso` (autenticación).
- `Proyecto` y `Participa` (+ `ParticipaId`) en `proyecto`.
- `Elemento` con herencia `JOINED` y subclases `HistoriaUsuario`, `Epica`, `Directorio`; autorreferencia `idElementoPadre` para la jerarquía (base del patrón Composite).
- Relaciones: `TienePermiso` (+ `TienePermisoId`) y `EsAsociado` (+ `EsAsociadoId`), modeladas como entidades con clave compuesta.
- **Cambio en `bd.md`:** `TienePermiso` ahora relaciona usuario, permiso y proyecto (`FK_idUsuario`, `FK_idPermiso`, `FK_idProyecto`); el permiso es por proyecto. Actualizados `TienePermiso` y `TienePermisoId` en consecuencia.
- Se usa Lombok para getters/setters/constructores.

## Persistencia (implementado)
- `application.properties` configurado para **MySQL**: `jdbc:mysql://localhost:3306/backlog` (crea la base si no existe), usuario `root`, contraseña `1234`, driver `com.mysql.cj.jdbc.Driver`, `ddl-auto=update`, dialecto `MySQLDialect` y `show-sql=true`.
- Dependencia `mysql-connector-j` ya presente en `pom.xml` (scope `runtime`).
- Se recomienda sobrescribir credenciales por variables de entorno en producción (pendiente).

## Repositorios (implementado)
- Un `JpaRepository` por entidad en el paquete `repository` de cada módulo:
  - `registro`: `UsuarioRepository`.
  - `autenticacion`: `PermisoRepository`, `TienePermisoRepository`.
  - `proyecto`: `ProyectoRepository`, `ParticipaRepository`.
  - `elemento`: `ElementoRepository`, `HistoriaUsuarioRepository`, `EpicaRepository`, `DirectorioRepository`, `EsAsociadoRepository`.
- Claves compuestas: `TienePermisoId`, `ParticipaId`, `EsAsociadoId`.

## Registro (implementado)
- `RegistroController` (`/api/registro`, `POST`) usa `@Valid @RequestBody RegistroRequest` y devuelve `201 Created`.
- `RegistroRequest` (DTO) recibe `username`, `email`, `password` y `password_confirm` (mapeado con `@JsonProperty`), con validaciones Jakarta.
- `UsuarioService` (`registro/services`) contiene la lógica: valida datos, evita duplicados de usuario/correo, hashea la contraseña con `PasswordEncoder` y persiste.
- El campo `fechaNacimiento` no se solicita en el formulario de `docs/UI-UX`; queda nulo al registrar.
- **Corrección detectada y solucionada:** el endpoint original era `POST /api/registro/registrarse` (no respetaba el ejemplo “sin modificar” que esperaba `POST /api/registro`). Se unificó a `POST /api/registro` para coincidir con `environment.ts` y `SecurityConfig.permitAll("/api/registro/**")`. Confirmado con `curl`: `201 Created`.

## Preferencias de código (correcciones del usuario)
- Usar **`@Autowired`** (inyección en campos) sobre repositorios y servicios.
- Los controladores reciben datos con **`@RequestBody`** (JSON), no con `@ModelAttribute`.
- Priorizar anotaciones y enfoques conocidos/familiares para el usuario.

## CORS y errores (implementado)
- `SecurityConfig` habilita CORS y expone `CorsConfigurationSource` para `http://localhost:4200` y `http://127.0.0.1:4200` (métodos `GET, POST, PUT, DELETE, OPTIONS`, headers `*`, credenciales permitidas).
- `exception/GlobalExceptionHandler` (`@RestControllerAdvice`): validaciones y `IllegalArgumentException` → `400` con `{ "message": ..., "errors": ... }`; fallos inesperados → `500`.

## Frontend Angular (implementado)
- Angular 22 standalone con SSR, Tailwind v4 y `provideHttpClient()`.
- Estructura respetada: `models`, `services`, `pages`, `components`; `guards` e `interceptors` quedan reservados para autenticación.
- Página `pages/registro/registro` transcrita desde `docs/UI-UX/registro_de_usuario_gil.html`, con Reactive Forms, validaciones, toggle de contraseña, coincidencia de contraseñas y toast.
- `components/toast` es reutilizable y recibe `message` y `type`.
- `services/registro.service.ts` consume `POST http://localhost:8081/api/registro` usando `environment.apiUrl` (`src/environments/environment.ts`).
- Contrato enviado: `{ username, email, password, password_confirm }` (coincide con el DTO del backend).
- Tras un `201`: toast de éxito y limpieza del formulario. Ante error: se muestra `error.error.message`.
- Alta del backend: puertos `8080` y `8081` ocupados por otros procesos; el backend corre en **`8081`** (`server.port=8081` en `application.properties`); el front responde en **`4200`**.
-visual: descripciones de campos (`Se usará para menciones...`, etc.) y subtítulo de título (`Únete a tu equipo...`) eliminadas de `registro.html`.
- Rutas: `''` redirige a `/registro`; `/login`, `/registro` y `/dashboard` (protegido) están disponibles; wildcard vuelve a `/registro`.
- Tokens de diseño (colores, tipografías, tamaños, radios, spacing) migrados a `@theme` en `src/styles.css`; fuentes y Material Symbols en `src/index.html`.
- Verificado con `npm run build` y `npm test`.

## Autenticación / Login (implementado)
- **Criterio del usuario:** cada autenticación debe generar un token usando lo ya implementado (`JwtService.generateToken`). El token se crea en cada login, no se reutiliza.
- `AuthController` (`/api/auth`, `POST /login`) recibe `@Valid @RequestBody LoginRequest` y devuelve `200 OK` con `LoginResponse` (`{ "token": "<jwt>" }`).
- `LoginRequest` (`autenticacion/dto`): `username` y `password`, con validaciones Jakarta; no se pide email ni confirmación (según `docs/UI-UX/acceso_registro_gil.html`).
- `AuthService` (`autenticacion/services`): valida entradas, busca el usuario con `UsuarioRepository.findByUsername`, compara la contraseña con `PasswordEncoder.matches` y genera el JWT con `JwtService.generateToken(username)`.
- No se usa `AuthenticationManager` para el login: `Usuario` no implementa `UserDetails` y no hay `UserDetailsService`, por eso la validación es directa contra el repositorio.
- Credenciales inválidas lanzan `IllegalArgumentException` → `400` con `{ "message": "Credenciales inválidas" }` vía `GlobalExceptionHandler` (evita caer en el `500` genérico).
- El cliente debe enviar el token como `Authorization: Bearer <token>`; CORS ya expone el header `Authorization`.

## Acceso (login) en frontend (implementado)
- Página `pages/login` transcrita desde `docs/UI-UX/acceso_registro_gil.html` (panel "Iniciar Sesión"): pestañas superiores, campo `username` con icono `person`, `password` con icono `lock` y toggle, enlace "¿Olvidaste tu contraseña?", checkbox "Recordar esta sesión", botón rojo "Acceder a AgileFlow", pie "Regístrate aquí" y franja inferior "Autenticación cifrada para equipos ágiles / Scrum • Kanban".
- Se mantuvo la arquitectura existente: **página de login separada** de `/registro` (no se unificaron las pestañas en una sola pantalla). La pestaña "Crear Cuenta" y los enlaces de registro usan `routerLink="/registro"`; la ruta `/login` antes caía en el wildcard y ahora resuelve.
- Formulario reactivo con `FormBuilder.nonNullable.group`, `@if` para errores y `app-toast` reutilizable (mismo patrón que `pages/registro`).
- `models/login-request.model.ts` (`username`, `password`) y `models/login-response.model.ts` (`token`); `services/auth.service.ts` consume `POST {environment.apiUrl}/auth/login`.
- El token devuelto se guarda bajo la clave `authToken` (y el nombre en `authUsername`): en `localStorage` si se marca "Recordar esta sesión", si no en `sessionStorage`. El interceptor `interceptors/auth.interceptor.ts` lo envía como `Bearer` (ver sección Dashboard).
- `<title>` de `index.html` cambiado a "Acceso y Registro — AgileFlow" para coincidir con el prototipo.

## Dashboard de repositorios (implementado)
- **Criterio del usuario:** los repositorios/proyectos mostrados deben provenir de la base de datos; no se muestran datos hardcodeados. Un repositorio aparece solo cuando un usuario autenticado lo crea.
- Página `pages/dashboard/dashboard` transcrita desde `docs/UI-UX/dashboard_agil_repositorios.html` (header, sidebar, KPIs, grid de repositorios, artefactos, drawer de chat y modales).
- Backend:
  - `ProyectoController` (`/api/proyectos`): `GET` lista repositorios propios, `GET /compartidos` lista participaciones ajenas y `POST` crea un repositorio con el usuario autenticado como propietario (`201 Created`).
  - `ProyectoService` valida acceso por propietario o participación (`Participa`) y calcula contadores de épicas e historias desde los elementos persistidos.
  - `ElementoController` (`/api/proyectos/{projectId}`): `GET /elementos` y `POST /epicas`.
  - `ElementoService` valida el acceso al proyecto y crea `Epica`; el listado conserva los tipos ya persistidos (`DIRECTORIO`, `HISTORIA`, `EPICA`).
  - DTOs: `ProyectoRequest/Response`, `EpicaRequest`, `ElementoResponse`.
- Frontend:
  - `models/proyecto.model.ts` y `models/elemento.model.ts`; `services/proyecto.service.ts` y `services/elemento.service.ts`.
  - `interceptors/auth.interceptor.ts` agrega `Authorization: Bearer` a las llamadas de `environment.apiUrl`; registrado en `app.config.ts` con `withInterceptors`.
  - `guards/auth.guard.ts` protege `/dashboard` y redirige a `/login?returnUrl=...` si no hay sesión válida (ver sección Protección de rutas).
  - `pages/dashboard` muestra KPIs calculados (repos propios/compartidos, épicas, historias), pestañas "Mis Carpetas"/"Compartido conmigo", artefactos del repositorio seleccionado y estados vacíos cuando no hay datos.
  - El modal de repositorio solo envía campos existentes en `bd.md`; se omitieron metodología, visibilidad, responsable, story points, estado de roadmap, OKR y DoD por no existir en el modelo relacional.
  - El chat del drawer queda como UI local (sin persistencia) porque el chat se almacenará en una BD no relacional separada.
  - Login: tras `200`, guarda `authToken` y `authUsername` en `localStorage` o `sessionStorage` según "Recordar esta sesión" y navega a `/dashboard`.
  - Rutas actuales: `''` → `/registro`; `/login`, `/registro` y `/dashboard` (con guard); wildcard → `/registro`.
- Verificación: `npm run build`, `npm test` (2 tests) y pruebas con `curl` de registro, login, creación de proyecto/sub-carpeta/épica/historia y listados propios/compartidos.
- **Observación (resuelta):** la tabla `participa` generaba columnas duplicadas (`idUsuario`/`id_usuario`, `idProyecto`/`id_proyecto`) por la combinación de `@EmbeddedId` + `@MapsId`. Se corrigió el mapeo y el esquema físico (ver sección "Columnas duplicadas en claves compuestas").

## Protección de rutas y sesión (implementado)
- **Criterio del usuario:** bloquear el acceso al dashboard si no hay una sesión iniciada; la lógica debe vivir en `guards` y en lo relacionado con la protección de rutas.
- `services/session.service.ts` centraliza la sesión: guarda/lee/limpia `authToken` y `authUsername` en `localStorage` o `sessionStorage`, valida la expiración del JWT (`exp`) sin verificar la firma y es seguro en SSR (`isPlatformBrowser`).
- `guards/auth.guard.ts` (aplicado a `/dashboard`): si no hay sesión válida, redirige a `/login` con `returnUrl` de la ruta solicitada.
- `guards/guest.guard.ts` (aplicado a `/login` y `/registro`): si ya hay sesión activa, redirige a `/dashboard`.
- `app.routes.server.ts` marca `/dashboard` como `RenderMode.Client`: el servidor ya no prerenderiza el dashboard sin sesión; entrega el shell y el guard del cliente decide. La build ahora prerenderiza solo 2 rutas estáticas.
- `interceptors/auth.interceptor.ts` obtiene el token desde `SessionService` (valida expiración) en lugar de leer el almacenamiento directamente.
- Login: usa `SessionService.save(...)` y, tras el aviso de éxito, navega al `returnUrl` validado (solo rutas internas) o a `/dashboard`.
- Dashboard: `logout()` usa `SessionService.clear()`; el nombre de usuario se lee desde `SessionService`.
- Verificación: `npm run build`, `npm test` (10 tests: guards, sesión y app) y navegación headless sin sesión; `/dashboard` termina renderizando el login.

## Reducción de funcionalidades del dashboard (implementado)
- **Criterio del usuario:** las opciones "dividir el proyecto en sub-carpetas" y "+ Historia" no deben existir; tampoco el botón "+ Crear" que está junto al botón del chat. Se elimina también lo que se hubiera tocado en el backend para esas funciones.
- Frontend eliminado:
  - Botón `btn-quick-create` y todo su menú desplegable ("Nuevo Repositorio", "Nueva Épica", "Historia de Usuario") junto con `createMenuOpen` y `toggleCreateMenu`.
  - Botones de la tarjeta de repositorio "Dividir en sub-carpeta" y "+ Historia"; el único botón restante es "Abrir repositorio completo".
  - Botón "+ Plantilla Historia" del encabezado, botón "+ Historia (INVEST)" del panel de artefactos y la columna "Sub-carpeta" de la tabla.
  - Botones "+ Plantilla Épica" del encabezado y "+ Épica (Lean)" del panel de artefactos (ver sección siguiente).
  - Toda la sección "Sub-carpetas en …" (incluido "Crear sub-carpeta") y los modales `subrepo` y `story`.
  - Formularios `subrepoForm`/`storyForm`, métodos `openSubrepoModal`, `openStoryModal`, `submitSubrepo`, `submitStory`, `storyCountFor` y los computeds `subcarpetas`, `ownedWithSubfolders` y `subfolderCoverage`.
  - Modelos `CrearDirectorioRequest` y `CrearHistoriaRequest`, y los métodos `crearDirectorio`/`crearHistoria` de `ElementoService`.
  - `Proyecto.subcarpetas` desaparece del modelo; la tarjeta muestra ahora `épicas • historias` y el KPI "Repos Propios" muestra "Creados por ti".
- Backend eliminado:
  - `POST /api/proyectos/{projectId}/directorios` y `POST /api/proyectos/{projectId}/historias` en `ElementoController`.
  - `ElementoService.createDirectory`, `createStory`, `getParentElement` y `resolveIdentifier` (autogeneración de `US-N`); el servicio solo consulta elementos y crea épicas.
  - DTOs `DirectorioRequest` y `HistoriaRequest`, y `DirectorioRepository.countByProject_Id`.
  - `ProyectoResponse.subcarpetas` y el `setSubcarpetas(...)` en `ProyectoService`; la respuesta ahora solo expone `epicas` e `historias`.
  - Se conservan las entidades y repositorios de `Directorio`/`HistoriaUsuario` del modelo `bd.md` para leer datos ya persistidos; no se altera el modelo relacional.
- Verificación: `./mvnw -DskipTests compile` y `npm run build`, `npm test` (10 tests) pasan; por API el listado de proyectos ya no devuelve `subcarpetas`, `POST /epicas` sigue en `201` y las rutas eliminadas ya no existen.

## Plantillas de épicas e historias fuera del dashboard (implementado)
- **Criterio del usuario:** los botones que abren las plantillas de épicas y de historias de usuario solo se verán una vez que se entre a la página de un proyecto/directorio; por ahora se quitan del dashboard.
- Se eliminan del dashboard los botones "+ Plantilla Épica" y "+ Épica (Lean)" (los de historias ya se habían removido), además del modal `epic`, que quedaba inalcanzable.
- Se limpia la lógica del dashboard asociada: `ModalType` ya solo es `'repo' | null`, y se eliminan `targetProjectId`, `epicForm` y `submitEpic`.
- Se conservan, para la futura página de proyecto/directorio, el endpoint `POST /api/proyectos/{projectId}/epicas`, el DTO `EpicaRequest`, `ElementoService.createEpic` en backend y el método `crearEpica` de `frontend/src/app/services/elemento.service.ts`.
- No se modifica el modelo relacional de `docs/base-de-datos/bd.md`.

## Detalle de repositorio y plantillas (implementado)
- **Criterio del usuario:** convertir `docs/UI-UX/detalle_de_repositorio_permisos.html` en páginas Angular con menos carga visual, y separar los botones de creación de Historia y Épica; cada botón abre su plantilla en una página propia.
- Frontend:
  - Página `pages/repositorio/repositorio` en la ruta protegida `/repositorios/:id`: encabezado compacto, árbol lateral de contenido, tarjeta del repositorio, filtros Todos/Épicas/Historias, buscador y grid de artefactos. Se eliminaron del prototipo las sub-carpetas, versiones, sprint, leyendas y acciones que no tienen soporte en el modelo.
  - Botones separados "Nueva historia" (azul) y "Nueva épica" (púrpura) que navegan a `/repositorios/:id/historias/nueva` y `/repositorios/:id/epicas/nueva`.
  - Panel derecho "Usuarios y permisos" de **solo lectura**: propietario, acceso del usuario (propietario/participante) y contadores. No inventa participantes ni persistencia porque aún no hay endpoints.
  - `pages/epica-plantilla`: formulario con título y descripción; guarda con `POST /epicas`.
  - `pages/historia-plantilla`: plantilla basada en `docs/UI-UX/plantilla_historia_de_usuario.html` con título, rol/quiero/para, reglas de negocio y escenarios Dado/Cuando/Entonces dinámicos; compone la descripción y los criterios según `agent/criterios.md` y guarda con `POST /historias`.
  - `ProyectoService.obtener` (`GET /proyectos/{id}`) y `ElementoService.crearHistoria` (`POST /proyectos/{id}/historias`).
  - El dashboard ahora navega al detalle al abrir un repositorio (propio o compartido).
  - Rutas de `repositorios/**` marcadas como `RenderMode.Client` en `app.routes.server.ts`.
- Backend:
  - `GET /api/proyectos/{projectId}` devuelve el repositorio accesible con contadores (`ProyectoService.getById`).
  - `POST /api/proyectos/{projectId}/historias` con `HistoriaRequest` y `ElementoService.createStory`: valida el título, vincula la épica padre solo si pertenece al mismo repositorio y genera el identificador `US-NNN` único.
  - `ElementoService.listByProject` ahora consulta `Directorio`, `Epica` e `HistoriaUsuario` por separado y ordena por id; así el `tipo` llega correcto aunque la consulta polimórfica base devolviera `ELEMENTO`.
  - Se amplió la longitud física de columnas de texto (`Elemento.titulo/descripcion`, `HistoriaUsuario.identificador/reglasNegocio/criteriosAceptacion`) para admitir el contenido de las plantillas, sin cambiar campos ni relaciones del modelo lógico.
- Verificación: `./mvnw -DskipTests compile`, `npm run build` y `npm test -- --watch=false` (10 tests) pasan; por API se creó repositorio, épica e historia, y `GET /elementos` devuelve los tipos `EPICA` e `HISTORIA` correctamente.

## Estado de historia y criterios de aceptación (corrección del usuario)
- **Criterio del usuario:** la plantilla de historia debe incluir un campo de estado debajo de "Épica asociada", con los valores `aprobada`, `rechazada` y `en desarrollo`; además, los criterios de aceptación deben ser un único campo de texto grande con un placeholder de ejemplo `Dado ..., cuando ..., entonces ...`.
- Se agregó `estado` a `HistoriaDeUsuario` (columna `estado`, longitud 20) y a `HistoriaRequest`/`ElementoResponse`; `ElementoService.createStory` valida que el valor sea exactamente `aprobada`, `rechazada` o `en desarrollo`.
- `docs/base-de-datos/bd.md` se actualizó con el campo `estado` por pedido explícito del usuario (excepción a la regla de no modificar el modelo).
- La plantilla `historia-plantilla` muestra un selector de estado debajo del selector de épica (por defecto "En desarrollo") y reemplaza los escenarios dinámicos por un textarea grande obligatorio con el formato Dado/Cuando/Entonces; el texto se envía tal cual en `criteriosAceptacion`.
- El detalle del repositorio muestra el estado como etiqueta en las tarjetas de historia.

## Identificador y título de la plantilla de historia (corrección del usuario)
- **Criterio del usuario:** se conservan los cambios hechos directamente en la plantilla (`1. Identificador` y `2. Titulo`). El valor escrito en "Identificador" se guarda tal cual en el atributo `identificador` de `HistoriaUsuario` (columna `identificador` del modelo); **no** es el id autoincremental de la base de datos ni se autogenera como `US-NNN`.
- El campo "Titulo" recibe el mismo tratamiento que "Criterios de aceptación": un único textarea con placeholder de ejemplo cuyo texto se envía tal cual, pero más chico (`rows=4`, `min-h-24` frente a `rows=10`, `min-h-56`). Se eliminan los campos `rol`, `requerimiento` y `beneficio` y la composición de la descripción.
- Backend: `HistoriaRequest` incorpora `identificador` (obligatorio, máximo 50) y ya no recibe `descripcion`; `ElementoService.createStory` usa `requireIdentifier(...)` en lugar del generador `nextIdentifier(...)` (eliminado). Frontend: `CrearHistoriaRequest` incorpora `identificador`.

## Botón "+directorio" en el detalle del repositorio (implementado)
- **Criterio del usuario:** en la página que muestra el contenido de un directorio (doble clic en un proyecto) se agrega un botón "Nuevo directorio" al lado de "Nueva historia" y "Nueva épica". Al pulsarlo abre un **modal** para escribir el nombre y crear el subdirectorio (opción elegida por el usuario entre modal / página de plantilla / solo visual).
- Backend: se rehabilita `POST /api/proyectos/{projectId}/directorios` con `DirectorioRequest` (solo `titulo`, obligatorio y máximo 150) y `ElementoService.createDirectory`, que valida acceso al proyecto, crea `Directorio`, asigna fechas y devuelve `ElementoResponse` (`201`). No se modifica el modelo relacional; `Directorio` ya existía en `bd.md`.
- Frontend: `CrearDirectorioRequest` y `ElementoService.crearDirectorio`; `pages/repositorio` gestiona `directorioForm`, `modalOpen`, `saving` y recarga la lista tras crear. El botón aparece en el encabezado y en el estado vacío.
- Los directorios vuelven a mostrarse en el listado (filtros "Directorios" en barra lateral y pestañas, tarjetas con icono de carpeta y árbol lateral), porque antes `filtered()` solo aceptaba épicas e historias y el directorio creado no era visible.

## Permisos por proyecto con TienePermiso (corrección del usuario, implementado)
- **Criterio del usuario:** la tabla `TienePermiso(FK_idUsuario, FK_idPermiso, FK_idProyecto)` define los permisos de un usuario invitado sobre un proyecto específico, para crear, leer, modificar y eliminar elementos (historias, épicas y sub-directorios). El usuario eligió **enforzar solo en los endpoints existentes**, sin agregar endpoints de invitación ni CRUD de elementos.
- `PermisoAccion` (`autenticacion/entitys`): enum `LEER`, `CREAR`, `MODIFICAR`, `ELIMINAR`; traduce el campo `tipo` de `Permiso` con alias en español (sin acentos) e inglés, y `todos`/`admin` habilita las cuatro acciones. `MODIFICAR` y `ELIMINAR` quedan disponibles para futuras operaciones de edición/borrado.
- `PermisoService` (`autenticacion/services`): consulta `TienePermisoRepository.findByIdUserIdAndIdProjectId`, aplica la traducción anterior y hace bypass total para el propietario. `requirePermission` lanza `PermisoDenegadoException` → HTTP **403** con `{ "message": ... }` vía `GlobalExceptionHandler`.
- **Patrón Proxy (exigido en `agent/criterios.md`):** `elemento/services/proxy/ElementoAccessProxy` valida `LEER` en el listado y `CREAR` en las tres altas antes de delegar en `ElementoService`; `ElementoController` inyecta el proxy, no el servicio directo.
- `ProyectoService.getAccessibleProject` ahora acepta propietario, participante (`Participa`) **o** usuario con al menos un permiso en `TienePermiso`, para que un invitado con permisos pueda operar sobre los elementos. `listShared()` sigue basado en `Participa`.
- No se modifica `docs/base-de-datos/bd.md`; las entidades `Permiso`, `TienePermiso` y `TienePermisoId` ya existían. No se crean permisos automáticamente: las filas de `TienePermiso` se cargan por carga de datos (no hay endpoint de invitación, según la decisión del usuario).

## Ver / Modificar / Eliminar elementos del repositorio (implementado)
- **Criterio del usuario:** cada elemento del listado (directorio, épica o historia) debe ofrecer "Ver", "Modificar" y "Eliminar", todas pasando por el proxy de permisos del backend; al modificar se cargan los valores existentes en el formulario y se pueden guardar los cambios.
- Backend: `GET`, `PUT` y `DELETE /api/proyectos/{projectId}/elementos/{elementId}` en `ElementoController`; `ElementoAccessProxy` exige `LEER`, `MODIFICAR` y `ELIMINAR` respectivamente antes de delegar. `ElementoService` implementa `getById`, `updateElement` y `deleteElement`, busca el elemento por subtipo para conservar su tipo polimórfico, valida que pertenezca al proyecto y actualiza campos comunes y específicos (historia: identificador, reglas, criterios, estado y épica; directorio: color). El borrado se bloquea si el elemento tiene hijos y limpia antes las filas de `EsAsociado`.
- DTO nuevo: `ElementoUpdateRequest` (campos opcionales por tipo, con validación Jakarta). No se agregaron campos al modelo relacional.
- Frontend: `ActualizarElementoRequest` y los métodos `obtenerElemento`, `actualizarElemento` y `eliminarElemento` de `ElementoService`. La página `pages/repositorio` usa un único `modal` (`directorio | ver | editar | eliminar`), carga el elemento con `GET` antes de mostrar el detalle o el formulario de edición, y recarga el listado tras guardar o borrar. Los errores 403 del proxy se muestran en el toast.
- **Corrección del usuario:** al presionar "Modificar" en una **historia de usuario** ya no se abre el modal: se navega a `repositorios/:id/historias/:historiaId/editar`, que reutiliza `pages/historia-plantilla`. La plantilla detecta el parámetro, carga la historia y rellena los mismos campos de creación (quitando el prefijo `- ` de las reglas al mostrarlas) y guarda con `PUT`, manteniendo el título "Modificar Historia de Usuario" y el botón "Guardar cambios". Directories y épicas siguen usando el modal.
- `ActualizarElementoRequest.descripcion` es opcional y `ElementoService.updateElement` solo la sobrescribe cuando se envía, para que la plantilla de historia no borre la descripción existente.

## Concurrencia optimista (implementado)
- **Criterio del usuario:** prevenir que dos usuarios modifiquen simultáneamente el mismo elemento; usar `@Version` de JPA con Hibernate.
- `Elemento` tiene `@Version Long version` (columna `version` con `BIGINT DEFAULT 0`, agregada a `bd.md` por petición explícita, igual que `estado`).
- `ElementoResponse` expone `version` y `ElementoUpdateRequest` exige `version` (`@NotNull`), por lo que el cliente debe reenviar la versión que leyó.
- `ElementoService.updateElement` compara la versión recibida contra la persistida antes de aplicar cambios; si difiere lanza `ConflictoConcurrenciaException` → **409 Conflict**.
- `GlobalExceptionHandler` también traduce `ObjectOptimisticLockingFailureException` y `OptimisticLockException` de Hibernate a **409** con mensaje en español.
- El proxy de permisos sigue verificando `MODIFICAR` antes de delegar; la concurrencia optimista no sustituye los permisos.
- Frontend: `Elemento` y `ActualizarElementoRequest` incluyen `version`; el modal del repositorio y la plantilla de historia envían la versión cargada al guardar. El mensaje 409 se muestra en el toast existente.
- Nota: al existir filas previas, la columna se crea con valor 0 por el `DEFAULT`; Hibernate incrementa la versión en cada `UPDATE` de elementos.

## Colores de estado de historia (implementado)
- **Criterio del usuario:** etiqueta verde para `aprobada`, roja para `rechazada` y naranja para la historia en curso (`en desarrollo`; también se acepta el alias `en proceso`).
- `pages/repositorio` usa `estadoBadgeClass(estado)` para colorear tanto la etiqueta de la tarjeta como el detalle del elemento; el color por defecto sigue siendo gris.

## Navegación a subdirectorios (implementado)
- **Criterio del usuario:** abrir un subdirectorio es idéntico a abrir un proyecto: misma vista, mismas acciones y solo los **hijos directos** del directorio; los elementos creados desde esa vista quedan dentro del directorio abierto.
- Se usa el mismo componente `pages/repositorio` con el query param `directorioId` (sin nuevas rutas). El `queryParamMap` se observa en el constructor para recargar al navegar entre directorios.
- Backend: `GET /api/proyectos/{projectId}/elementos?parentId={id}` lista los hijos directos validando que el padre sea un `Directorio` del proyecto; pasa por el proxy con permiso `LEER`.
- Los DTOs de creación (`DirectorioRequest`, `EpicaRequest`, `HistoriaRequest`) y `ElementoUpdateRequest` incorporan `idElementoPadre` (ya existente en el modelo). En historias, el directorio tiene precedencia sobre `idEpicaPadre`; si no hay directorio se mantiene la épica padre.
- El árbol lateral sigue mostrando todos los directorios del proyecto (para navegar entre hermanos) mientras la grilla muestra solo el contenido del directorio abierto; se agregó miga de pan y "Volver al repositorio".
- Verificado por API: crear subcarpeta y épica con `idElementoPadre` → `201`, y `?parentId=` devuelve solo esos dos hijos. `mvnw test`, `npm run build` y `npm test` (10) en verde.
- **Corrección del usuario:** abrir un directorio debe navegar a **otra página** (ruta propia), no al mismo URL con query param. Ruta `/repositorios/:id/directorios/:directorioId`, que renderiza el mismo componente `Repositorio` para conservar la vista idéntica.
- En las tarjetas se agregó el botón **"Abrir"** antes de Ver/Modificar/Eliminar (solo para `DIRECTORIO`) y doble clic en la tarjeta hace lo mismo. Épicas e historias no tienen página propia: solo conservan Ver/Modificar/Eliminar.
- Las plantillas siguen recibiendo `directorioId` por query param, pero al guardar/volver navegan a la ruta nueva del directorio. El componente `Repositorio` observa `paramMap` y `queryParamMap` para sostener la ruta.
- **Corrección del usuario (alcance de hijos directos):** la página de un directorio muestra **solo sus hijos directos**, nunca hermanos ni nietos; el repositorio raíz se comporta igual (solo elementos con `parent == null`).
- `ElementoService.listByProject` filtra ahora los elementos raíz; el árbol lateral de `pages/repositorio` usa los mismos elementos de la vista (ya no consulta el proyecto completo), por lo que no aparecen elementos hermanos.
- Verificado por API con datos persistidos: raíz del proyecto devuelve solo `Carpeta A`, y `?parentId=13` devuelve solo `Subcarpeta A1` y `Epica dentro de A`. `mvnw test`, `npm run build` y `npm test` (10) en verde.

## Borrado recursivo con Composite (implementado)
- **Criterio del usuario:** en la carpeta del patrón Composite se debe borrar de forma recursiva; al eliminar un subdirectorio (o el proyecto) se envía el mismo mensaje `delete()` a todos los elementos hijos.
- Se implementó `elemento/composite`: `BacklogComponent` (interfaz con `delete()`), `HojaElemento` (hoja) y `DirectorioComposite` (compuesto que propaga a los hijos y luego se borra; con directorio `null` representa la raíz del proyecto).
- `BacklogDeletionService` (también en `composite`) arma el árbol desde `ElementoRepository.findByParent_Id`, limpia `EsAsociado` antes de borrar cada elemento y, para un proyecto, borra raíz → hijos → `Participa` → `TienePermiso` → `Proyecto`. Evita dependencia circular porque solo inyecta repositorios; `ElementoService` y `ProyectoService` lo invocan.
- `ElementoService.deleteElement` ya no bloquea si hay hijos: delega en el Composite (sigue pasando por el proxy con permiso `ELIMINAR`).
- Nuevo `DELETE /api/proyectos/{projectId}` (204), solo para el propietario; si no lo es devuelve 403 vía `PermisoDenegadoException`.
- Se agregó `DirectorioCompositeTest` (verifica orden hijos antes que directorio). Verificado por API: borrar directorio con subdirectorio + historia → 204 y la raíz queda vacía; borrar proyecto con carpeta + épica → 204 y el proyecto deja de existir. `mvnw test` en verde (4 clases de prueba).

## Inicio de sesión con Google (implementado)
- **Criterio del usuario:** añadir autenticación con Google al login existente; el usuario eligió **crear la cuenta en el primer acceso** y mostrar el botón **solo en la página de login**.
- **Enfoque:** Google Identity Services (GIS) en el frontend devuelve un **ID token**; el backend lo verifica y emite el **JWT propio** con `JwtService`. No se usa el flujo de redirección `oauth2Login`, por lo que la API sigue siendo stateless y `/api/auth/**` sigue público.
- **Backend:**
  - Dependencia `com.google.api-client:google-api-client` 2.9.2 (incluye `google-http-client-gson`, sin conflicto con Jackson 3 de Spring Boot 4).
  - Propiedad `security.google.client-id=${GOOGLE_CLIENT_ID:}` en `application.properties`.
  - `GoogleLoginRequest` (`credential`, `@NotBlank`) y `GoogleAccountInfo` (email, subject, fullName).
  - `GoogleTokenVerifier` verifica firma, audiencia, expiración y `email_verified` con `GoogleIdTokenVerifier`; si falta el Client ID responde con un mensaje claro.
  - `AuthService.loginWithGoogle`: busca la cuenta con `UsuarioRepository.findByEmailIgnoreCase`; si no existe la crea con `email` normalizado, `username` único derivado de la parte local del correo y contraseña BCrypt aleatoria (la cuenta solo se autentica con Google). Un `ConcurrentHashMap` de bloqueos por correo evita altas duplicadas en logins simultáneos.
  - `AuthController` expone `POST /api/auth/google` → `200` con `LoginResponse { token, username }`.
  - `LoginResponse` ahora incluye `username` (el login con Google no conoce el nombre de usuario del formulario).
  - Sin cambios en `docs/base-de-datos/bd.md` ni en `SecurityConfig`.
- **Frontend:**
  - `environment.googleClientId` (debe coincidir con `GOOGLE_CLIENT_ID` del backend).
  - `services/google-identity.service.ts`: carga perezosa del script `https://accounts.google.com/gsi/client`, inicializa GIS y dibuja el botón oficial; todo seguro para SSR (`isPlatformBrowser`).
  - `models/google-identity.model.ts` con los tipos de GIS y la declaración de `window.google`.
  - `pages/login`: `ngAfterViewInit` dibuja el botón si hay Client ID; el callback envía la credencial a `AuthService.googleLogin`, guarda el JWT en `SessionService` con el estado de "Recordar esta sesión" y navega al `returnUrl`/dashboard. Si no hay Client ID se muestra un aviso en el formulario.
  - `login.html` agrega el separador "o" y el contenedor del botón; `LoginResponse` del frontend incluye `username`.
- **Configuración externa:** en Google Cloud Console crear un OAuth Client ID tipo **Web application** con orígenes autorizados `http://localhost:4200` y `http://127.0.0.1:4200`; el *client secret* no se usa en este flujo.
- **Seguridad pendiente (mejora opcional):** el enlace por correo puede sufrir "pre-secuestro" porque el registro local no verifica el correo. Endurecimiento sin cambiar el esquema: guardar `encode("google:" + sub)` y exigir `matches` en logins posteriores.
- **Verificación:** `mvnw -Dtest=AuthServiceGoogleTest test` (5 pruebas) y `npm run build` (incluye prerender SSR) en verde.

## Eliminar repositorio desde el dashboard (implementado)
- **Criterio del usuario:** agregar en el listado de proyectos del dashboard una opción para eliminar el proyecto, con el botón al lado de "Abrir repositorio completo"; el borrado debe ser recursivo aplicando el patrón Composite.
- **Backend:** sin cambios. Ya existía `DELETE /api/proyectos/{projectId}` (204), que comprueba que el usuario autenticado sea el propietario (403 si no lo es) y delega en `BacklogDeletionService.deleteProjectTree`, que aplica el Composite: borra los hijos antes que el directorio raíz y luego `Participa`, `TienePermiso` y `Proyecto`.
- **Frontend:**
  - `ProyectoService.eliminar(projectId)` consume `DELETE /api/proyectos/{id}`.
  - En las tarjetas de repositorios **propios** se agregó un botón con icono `delete` junto a "Abrir repositorio completo"; no aparece en "Compartido conmigo" porque el backend solo permite borrar al propietario.
  - Modal de confirmación (`modal()` = `'delete'`) con el nombre del repositorio y aviso de que se eliminan recursivamente carpetas, épicas e historias; estado `deleting` para deshabilitar los botones mientras se procesa.
  - Al confirmar se muestra un toast, se recarga el listado y, si el repositorio eliminado estaba seleccionado, se selecciona otro o se vacía el panel de artefactos.
- **Verificación:** `npm run build` y `npm test -- --watch=false` en verde.

## Historia asociada a una épica en el listado (corregido)
- **Problema:** al asociar una historia a una épica, la historia desaparecía del listado del proyecto o del directorio.
- **Causa:** `createStory`/`updateElement` guardaban la épica como **padre jerárquico** (`Elemento.idElementoPadre`), así que `listByProject`/`listByParent` la ocultaban al no ser hija directa del repositorio/directorio.
- **Solución (sin cambios de esquema):** la épica pasa a guardarse en la tabla `EsAsociado` (historia ↔ épica) y `idElementoPadre` queda reservado al directorio contenedor.
- **Backend (`ElementoService`):**
  - `createStory`/`updateElement` resuelven el directorio con `resolveParentDirectory` y la épica con `resolveParentEpic`, y sincronizan `EsAsociado` (`syncStoryEpicAssociation`: una épica por historia, borra e inserta con `flush`).
  - `listByProject`, `listByParent` y `getById` calculan el contexto de cada historia: directorio padre real + épica asociada. Los datos antiguos que guardaron la épica como padre se siguen mostrando bajo el directorio de esa épica y conservan la épica como asociada.
  - `ElementoResponse` incluye `idEpica` y `tituloEpica`; `EsAsociadoRepository.findByProjectId` carga las asociaciones del repositorio en una sola consulta.
  - El borrado recursivo ya limpiaba `EsAsociado`, así que eliminar una épica desasocia sus historias en lugar de eliminarlas.
- **Frontend:**
  - `Elemento` incluye `idEpica`/`tituloEpica`; los formularios de historia usan `idEpica` para preseleccionar la épica (antes usaban `idElementoPadre`).
  - En el listado de cada directorio/proyecto, junto al estado de la historia se muestra `Épica: <título>` o `Sin épica asociada`.
  - El detalle de la historia siempre muestra la fila "Épica asociada" con el título o "Sin épica asociada".
- **Verificación:** `mvnw -Dtest=ElementoServiceHistoriaEpicaTest,ElementoServiceConcurrenciaTest test` (5 pruebas nuevas/totales en verde), `npm run build` y `npm test -- --watch=false`.

## Asociar cualquier épica del proyecto a una historia (corregido)
- **Problema:** al asignar una épica a una historia que estaban en **directorios distintos**, la épica no aparecía en el selector de la plantilla de historia; solo se listaban las épicas raíz del repositorio.
- **Causa:** `pages/historia-plantilla` cargaba las épicas con `GET /api/proyectos/{id}/elementos` sin `parentId`, que devuelve solo los hijos directos de la raíz.
- **Regla de negocio (criterio del usuario):** se puede asociar **cualquier épica del mismo proyecto**, sin importar el directorio; la única restricción es que épica e historia pertenezcan al mismo repositorio (ya validado por `ElementoService.resolveParentEpic`).
- **Backend:** nuevo `GET /api/proyectos/{projectId}/epicas` → `ElementoService.listEpicsByProject`, que devuelve todas las épicas del proyecto ordenadas por id; pasa por `ElementoAccessProxy` con permiso `LEER`. Sin cambios en el modelo relacional.
- **Frontend:** `ElementoService.listarEpicas(projectId)`; la plantilla de historia usa ese listado y muestra en cada opción el directorio de la épica (`tituloPadre`) o "raíz del repositorio" para desambiguar.
- **Verificación:** `mvnw test` (14 pruebas, incluidas 2 nuevas en `ElementoServiceHistoriaEpicaTest`), `npm run build` y `npm test -- --watch=false` (11 pruebas) en verde.

## Columnas duplicadas en claves compuestas (corregido)
- **Problema:** al asociar una épica de un directorio distinto a una historia, el `POST` y el `GET` del elemento respondían **500**. La asociación (`es_asociado`) no se podía insertar.
- **Causa raíz:** las tablas con clave compuesta (`es_asociado`, `participa`, `tiene_permiso`) se creaban con **columnas duplicadas**: la PK en camelCase (`idElementoHistoria`, `idElementoEpica`, `idUsuario`, …) y las FK en snake_case (`id_elemento_historia`, …). El `@Column` dentro del `@Embeddable` generaba columnas propias y el `@JoinColumn` del `@MapsId` generaba otras. Las columnas camelCase quedaban `NOT NULL` sin valor por defecto, así que el `INSERT` fallaba.
- **Solución (sin cambiar el modelo lógico de `bd.md`):**
  - Se quitaron los `@Column` de `EsAsociadoId`, `ParticipaId` y `TienePermisoId`; la columna física la define únicamente el `@JoinColumn` de la asociación con `@MapsId`.
  - Los `@JoinColumn` de `EsAsociado`, `Participa` y `TienePermiso` usan nombres físicos en snake_case (`id_elemento_historia`, `id_elemento_epica`, `id_usuario`, `id_proyecto`, `id_permiso`), coherentes con el resto del esquema.
  - Las tres tablas estaban vacías, así que se recrearon con el esquema limpio (Hibernate `ddl-auto=update` ya no agrega columnas duplicadas).
- **Importante:** `ddl-auto=update` no elimina columnas viejas. En cualquier otra base con las tablas duplicadas hay que **recrear** `es_asociado`, `participa` y `tiene_permiso` (vacías) y reiniciar el backend para que Hibernate las cree de nuevo.
- **Verificación:** esquema generado desde cero en una base temporal (`es_asociado(id_elemento_epica, id_elemento_historia)` como PK, sin duplicados); flujo por API registro → login → proyecto → directorio A → directorio B → épica en B → historia en A asociada a la épica de B → `201` y `GET` `200`; `mvnw test` (14 pruebas) en verde. El backend en `8081` debe reiniciarse para cargar el nuevo mapeo.

## Resumen de historias por épica en el panel del repositorio (implementado)
- **Criterio del usuario:** mostrar en el panel derecho del detalle del repositorio (donde están acceso, origen y contadores) la información de cada épica con las historias de usuario asociadas; debe verse siempre, incluso al entrar a un subdirectorio.
- **Backend:** nuevo `GET /api/proyectos/{projectId}/epicas/resumen` → `ElementoService.listEpicSummary`, que recorre **todo el proyecto** (no depende del directorio) y agrupa las historias por épica:
  - Usa `EsAsociadoRepository.findByProjectId` para las asociaciones actuales y, como respaldo, las historias antiguas que guardaban la épica como padre (`resolveEpicForStory`, compartido con `resolveStoryContext`).
  - Devuelve `EpicaResumenResponse` (`id`, `titulo`, `idElementoPadre`, `tituloPadre`, `totalHistorias`, `historias`) y `HistoriaResumenResponse` (`id`, `identificador`, `titulo`, `estado`). Las épicas sin historias también aparecen con `0`.
  - Pasa por `ElementoAccessProxy` con permiso `LEER`. Sin cambios en el modelo relacional.
- **Frontend:** `EpicaResumen`/`HistoriaResumen` en `elemento.model.ts`; `ElementoService.listarResumenEpicas`; `pages/repositorio` carga el resumen en el mismo `forkJoin` del proyecto (siempre, con o sin `directorioId`) y lo muestra en el panel derecho como "Historias por épica". El panel ahora tiene scroll vertical.
- **Corrección del usuario:** el panel se muestra como **árbol**: la épica es el nodo padre con el icono morado (`description`, `text-purple-600`) y su título (más el contador y el directorio en tooltip); sus hijos son las historias de usuario con el icono azul (`text-sky-600`) y su **identificador** como etiqueta (el título queda en tooltip). Las ramas se dibujan con borde y conector morados.
- **Despliegue del árbol:** cada épica tiene un chevron que despliega/contrae sus historias (`expandedEpics` en `pages/repositorio`); además hay un botón global "Expandir/Contraer" en la cabecera de la sección. Por defecto las épicas quedan **contraídas** para no saturar el panel cuando hay muchas historias. Las épicas sin historias muestran el aviso sin chevron.
- **Verificación:** por API, épica en "dir B" con 2 historias guardadas en "dir A" → `totalHistorias: 2` desde la raíz del proyecto; épica raíz sin historias → `0`. `mvnw test` (15 pruebas), `npm run build` y `npm test -- --watch=false` (11 pruebas) en verde.



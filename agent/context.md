## Descripcion
- Desarrollo de un software backlog y chat.El backlog es capaz de crear repostorios,historias de usuario,epicas,etc. Es decir que va permitir el trabajo aplicando metodologias agiles
## Lectura inicial obligatoria
Antes de proponer cambios o escribir código en una nueva sesión, lee y aplica:
- `agent/criterios.md`
- `agent/feedback.md`
en feeback van las correcciones, errores recurrentes y ajustes aprendidos al momento de ralizar el desarrollo del proyecto y cuando te responda a dudas
## Skills disponibles
Cuando la tarea corresponda a uno de estos casos, lee el `SKILL.md` indicado antes de actuar:
- **Páginas y componentes Angular (`frontend/`):** `skills/frontend_skill.md`
- **Creacion de endpoints en el servidor con spring boot:**`skills/backend_skill.md`
## Stack y tecnologias
- **Backend:** Java 21 con el framework springboot
- **Frontend:** version mas reciente de Angular
-  **Agente IA:** Scripts en Python comunicandose con Ollama en local

## Estrcutura de carpetas para servicio Backlog
```text
backlog-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/backlog/service/
│   │   │       ├── BacklogServiceApplication.java
│   │   │       ├── config/
│   │   │       │   └── SecurityConfig.java
│   │   │       ├── security/                 # Filtro JWT, extracción de token y usuario actual
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   ├── JwtService.java
│   │   │       │   └── CurrentUserProvider.java
│   │   │       ├── registro/                 # Funcionalidad: alta y registro de clientes/usuarios
│   │   │       │   ├── controller/
│   │   │       │   │   └── RegistroController.java
│   │   │       │   ├── services/
│   │   │       │   │   └── RegistroService.java
│   │   │       │   ├── entitys/
│   │   │       │   │   └── Usuario.java
│   │   │       │   ├── dto/
│   │   │       │   └── repository/
│   │   │       ├── autenticacion/            # Funcionalidad: login, validación de credenciales y JWT
│   │   │       │   ├── controller/
│   │   │       │   │   └── AuthController.java
│   │   │       │   ├── services/
│   │   │       │   │   └── AuthService.java
│   │   │       │   ├── entitys/
│   │   │       │   │   └── Permiso.java
│   │   │       │   ├── dto/
│   │   │       │   └── repository/
│   │   │       ├── proyecto/                 # Funcionalidad: proyectos, propietario y participantes
│   │   │       │   ├── controller/
│   │   │       │   │   └── ProyectoController.java
│   │   │       │   ├── services/
│   │   │       │   │   ├── ProyectoService.java
│   │   │       │   │   └── ParticipacionService.java
│   │   │       │   ├── entitys/
│   │   │       │   │   ├── Proyecto.java
│   │   │       │   │   └── Participa.java
│   │   │       │   ├── dto/
│   │   │       │   └── repository/
│   │   │       ├── elemento/                 # Funcionalidad: elementos, jerarquía y patrón Composite/Proxy
│   │   │       │   ├── controller/
│   │   │       │   │   └── ElementoController.java
│   │   │       │   ├── services/
│   │   │       │   │   ├── ElementoService.java
│   │   │       │   │   └── proxy/
│   │   │       │   │       └── ElementoAccessProxy.java
│   │   │       │   ├── entitys/
│   │   │       │   │   ├── Elemento.java
│   │   │       │   │   ├── HistoriaUsuario.java
│   │   │       │   │   ├── Epica.java
│   │   │       │   │   └── Directorio.java
│   │   │       │   ├── composite/
│   │   │       │   │   ├── BacklogComponent.java
│   │   │       │   │   ├── DirectorioComposite.java
│   │   │       │   │   └── HojaElemento.java
│   │   │       │   ├── dto/
│   │   │       │   └── repository/
│   │   │       └── exception/
│   │   │           └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/
│           └── com/backlog/service/
│               ├── registro/
│               ├── autenticacion/
│               ├── proyecto/
│               └── elemento/
├── pom.xml
└── README.md
```
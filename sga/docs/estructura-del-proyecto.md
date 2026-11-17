# Estructura del proyecto — SGA Aldea Bahareque

## Organización del repositorio

```
sga-aldea-bahareque/
├── README.md               Presentación del proyecto (empezar aquí)
├── sga/                    Sistema completo: backend, frontend, colección Bruno, documentación y despliegue
├── src/, docs/,            Primera entrega (Corte 1): dominio con sus pruebas, diseño de la API (openapi.yaml),
│   build.gradle, gradle/   libro de identificación, documento y diagramas de esa entrega
└── .gitignore, .gitattributes
```

- **`sga/` es el proyecto vigente.** Todo lo que se ejecuta y se despliega está ahí: Render construye desde `sga/` y Vercel desde `sga/frontend`.
- **La raíz conserva la primera entrega tal como se presentó.** Es la evidencia del Corte 1.
- Cada entrega se puede consultar en su etiqueta (`git checkout entrega-N`).

## Proyecto `sga/`

```
sga/
├── src/main/java/co/edu/uniquindio/sga/   Backend (Java 25, Spring Boot 4.1)
├── src/main/resources/                    Configuración por perfil
├── src/test/java/co/edu/uniquindio/sga/   Pruebas automáticas
├── frontend/                              Interfaz web (React 19 + Vite)
├── bruno/                                 Colección de peticiones y simulador del canal externo
├── docs/                                  Documentación de soporte
├── build.gradle, settings.gradle          Compilación con Gradle
├── gradlew, gradlew.bat, gradle/          Wrapper de Gradle (no hace falta instalarlo)
├── Dockerfile, .dockerignore              Imagen del backend para Render
└── render.yaml                            Blueprint de Render: PostgreSQL y servicio web
```

## Backend: arquitectura hexagonal

Las dependencias van de afuera hacia adentro: `infrastructure` → `application` → `domain`. El dominio no conoce Spring ni JPA. Justificación en [decisiones-tecnicas.md](decisiones-tecnicas.md#dt-01--arquitectura-hexagonal-puertos-y-adaptadores).

```
co.edu.uniquindio.sga
├── SgaApplication.java          Punto de entrada de Spring Boot
├── domain/                      Reglas del negocio y puertos (Java puro)
├── application/                 Casos de uso: orquestan el dominio
└── infrastructure/              Adaptadores: REST, JPA, seguridad e integraciones
```

### `domain` — reglas del negocio

| Paquete | Contenido principal | Puertos (interfaces que implementa la infraestructura) |
|---|---|---|
| `comun` | `Dinero`, `RangoFechas`, `Pagina`, `ExcepcionNegocio`, `CodigoError` | `Reloj`, `Notificador` |
| `alojamiento` | `Alojamiento`, `ParametrosAlojamiento`, `ServicioAdicional` | `AlojamientoRepositorio` |
| `apartamento` | `Apartamento`, `EstadoOperativo`, `Bloqueo`, `ImagenApartamento`, `Novedad` | `ApartamentoRepositorio`, `BloqueoRepositorio`, `NovedadRepositorio`, `AlmacenImagenes`, `SugeridorGravedad` |
| `tarifa` | `Temporada`, `CalendarioTemporadas`, `Tarifa`, `TablaTarifas` | `TemporadaRepositorio`, `TarifaRepositorio` |
| `politica` | `PoliticaCancelacion`, `TramoCancelacion` | `PoliticaRepositorio` |
| `reserva` | `Reserva` (agregado raíz), `EstadoReserva`, `Estancia`, `Ocupante`, `Titular`, `Canal`, `CotizadorEstancia`, `VerificadorDisponibilidad`, `GestorReservas` | `ReservaRepositorio` |
| `folio` | `Folio`, `Cargo`, `Pago`, `MedioPago`, `TipoCargo`, `AutorizacionCierre` | — (vive dentro de `Reserva`) |
| `canal` | `CanalVenta`, `ProcesadorReservasExternas`, `ConflictoCanal`, `EventoCanal` | `CanalVentaRepositorio`, `ConflictoCanalRepositorio`, `BitacoraCanalRepositorio` |
| `usuario` | `Usuario`, `Rol`, `CodigoRecuperacion` | `UsuarioRepositorio`, `CodigoRecuperacionRepositorio`, `CifradorContrasenas` |
| `recordatorio` | `Recordatorio`, `TipoRecordatorio`, `PlanificadorRecordatorios` | `RecordatorioRepositorio` |
| `reporte` | `CalculadoraReportes`, `Periodo`, `Reportes` | — |

### `application` — casos de uso

| Paquete | Servicios |
|---|---|
| `alojamiento` | `AlojamientoServicio` |
| `apartamento` | `ApartamentoServicio`, `ImagenServicio`, `SugerenciaGravedadServicio` |
| `tarifa` | `TarifaServicio` (temporadas, tarifas y políticas) |
| `reserva` | `DisponibilidadServicio`, `ReservaServicio`, `FolioServicio` |
| `canal` | `CanalServicio` (administración), `CanalExternoServicio` (contrato con las agencias) |
| `usuario` | `UsuarioServicio` (registro, ingreso, perfil, recuperación de contraseña) |
| `recordatorio` | `RecordatorioServicio` |
| `reporte` | `ReporteServicio` |
| `comun` | `Solicitante` (quién hace la petición y su rol), `AvisosReserva` (notificaciones no bloqueantes), `ContextoVigente` |

### `infrastructure` — adaptadores

| Paquete | Contenido |
|---|---|
| `rest/<agregado>` | Controladores REST con sus DTOs (`record`) y validaciones |
| `rest/comun` | `ManejadorErrores` (errores → `ErrorRespuesta`), `PaginaRespuesta`, `Respuestas` (traducción dominio → DTO) |
| `persistencia/<agregado>` | Entidades JPA (`*Entity`), repositorios de Spring Data y adaptadores (`*Adaptador`) que implementan los puertos |
| `seguridad` | `ConfiguracionSeguridad` (JWT, roles, CORS), `ClaveJwt`, `EmisorTokens`, `SolicitanteActual`, `CifradorBCrypt` |
| `notificacion` | `NotificadorEnMemoria` (registro) y `NotificadorCorreo` (SMTP) |
| `ia` | `SugeridorGravedadSimulado`, `SugeridorGravedadOpenAi` (API `/chat/completions`), `SugeridorConTiempoLimite`, `ConfiguracionIa` |
| `imagenes` | `AlmacenImagenesLocal`, `AlmacenImagenesCloudinary`, `ConfiguracionImagenes` |
| `configuracion` | `PropiedadesSga` (todas las propiedades `sga.*`), `ConfiguracionDominio` (beans del dominio), `TareaVencimientoReservas`, `TareaRecordatorios`, `DatosDePrueba`, `ConfiguracionFrontend` |

### Recursos

| Archivo | Uso |
|---|---|
| `application.properties` | Configuración base y de desarrollo (H2 en memoria, puerto 9090, integraciones locales) |
| `application-prod.properties` | H2 en archivo y clave JWT obligatoria |
| `application-postgres.properties` | PostgreSQL de Render, pool de 5 conexiones y clave JWT obligatoria |

## Pruebas (`src/test`)

| Carpeta | Qué prueba |
|---|---|
| `domain/<paquete>` | Las reglas de cada agregado: casos que cumplen y que violan cada RN y RP, patrón AAA, fechas fijas |
| `infrastructure/rest` | `ApiIntegracionTest`: la API completa con seguridad y persistencia |
| `infrastructure/...` | Integraciones (`IntegracionesApiTest`, `IaDeshabilitadaTest`, `SugeridorGravedadTest`), seguridad (`ClaveJwtTest`) y configuración (`CredencialesCloudinaryTest`) |

## Frontend (`sga/frontend`)

```
frontend/
├── index.html, public/       Página base e íconos
├── vite.config.ts            Servidor de desarrollo y proxy de /api al puerto 9090
├── vercel.json               Compilación y rutas en Vercel
├── .env.example              Variables (VITE_API_URL, VITE_MAPBOX_TOKEN)
└── src/
    ├── main.tsx, rutas.tsx   Arranque y rutas por rol
    ├── api/                  cliente.ts (fetch, token, errores), servicios.ts (endpoints), tipos.ts (contratos)
    ├── sesion/               Sesión JWT y RutaProtegida por rol
    ├── tema/                 Tema de Material UI (paleta Bahareque)
    ├── componentes/          Layout, avisos, componentes del negocio y novedades con IA
    ├── utilidades/           Formatos de pesos y fechas, estilos de estados
    └── paginas/
        ├── publico/          Inicio (búsqueda), detalle del apartamento, ingreso y registro, política
        ├── huesped/          Mis reservas y perfil
        ├── reserva/          Detalle de reserva, modificación y folio (compartido por roles)
        ├── recepcion/        Tablero del día, listado, reserva directa y ocupación
        ├── servicio/         Preparación de apartamentos y novedades
        └── admin/            Alojamiento, apartamentos, tarifas, usuarios, canales, reportes y recordatorios
```

## Colección Bruno (`sga/bruno`)

| Carpeta | Contenido |
|---|---|
| `01 Autenticacion` | Ingreso por rol; guarda los tokens en el entorno |
| `02 Publico` | Alojamiento, apartamentos, búsqueda y cotización |
| `03 Reservas` | Ciclo de vida de la reserva y folio |
| `04 Operacion diaria` | Llegadas, salidas, preparación y novedades |
| `05 Administracion` | Inventario, tarifas, políticas y usuarios |
| `06 Simulador canal externo` | Agencia externa: aceptada, duplicada, conflicto, credencial inválida y cancelación |
| `07 Integraciones y reportes` | IA, imágenes, reportes y recordatorios |
| `environments/local.bru` | Entorno local (`http://localhost:9090`) |

## Documentación (`sga/docs`)

| Documento | Contenido |
|---|---|
| [ficha-alojamiento.md](ficha-alojamiento.md) | Inventario, temporadas, tarifas, parámetros, políticas y reglas propias |
| [glosario.md](glosario.md) | Lenguaje ubicuo y su nombre en el código |
| [modelo-negocio.md](modelo-negocio.md) | Paquetes del dominio, ciclos de vida, decisiones de diseño y trazabilidad de reglas |
| [casos-de-uso.md](casos-de-uso.md) | Casos de uso por actor |
| [api.md](api.md) | Referencia de la API REST |
| [integraciones.md](integraciones.md) | Canal externo, correo, IA, imágenes, reportes y variables de entorno |
| [decisiones-tecnicas.md](decisiones-tecnicas.md) | Justificación de la arquitectura, las tecnologías y la infraestructura |
| [despliegue.md](despliegue.md) | Render, Vercel, H2 frente a PostgreSQL, arranque en frío y verificación |
| [solucion-de-problemas.md](solucion-de-problemas.md) | Errores frecuentes y cómo resolverlos |
| [estructura-del-proyecto.md](estructura-del-proyecto.md) | Este documento |
| `diagramas/` | Diagrama de clases y mapa de agregados |
| `entregas/`, `*.docx` | Documentos presentados en cada entrega y en la guía de seguridad |

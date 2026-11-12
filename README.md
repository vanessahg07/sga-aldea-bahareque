# SGA — Aldea Bahareque

![Java 25](https://img.shields.io/badge/Java-25-34533A)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-34533A)
![React 19](https://img.shields.io/badge/React-19-34533A)
![Pruebas](https://img.shields.io/badge/pruebas-263%20en%20verde-2F6B3E)
![Estado](https://img.shields.io/badge/cortes-1%20a%204%20en%20código-C9A55A)

**SGA - Sistema de Gestión de Alojamiento** para *Aldea Bahareque*, siete apartamentos turísticos independientes en Filandia, Quindío, en el Paisaje Cultural Cafetero. El sistema gestiona inventario, tarifas por temporada, disponibilidad, reservas, operación diaria y folios, y vende por tres canales (portal, directo y agencias externas) sin vender nunca dos veces la misma noche.

Proyecto final de **Programación Avanzada** — Programa de Ingeniería de Sistemas y Computación, Universidad del Quindío, 2026-2.

## Contenido

- [Integrantes](#integrantes)
- [Estado actual](#estado-actual)
- [División de las entregas](#división-de-las-entregas)
- [Qué hace el sistema](#qué-hace-el-sistema)
- [Tecnologías](#tecnologías)
- [Cómo ejecutar](#cómo-ejecutar)
- [Configuración](#configuración)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Pruebas](#pruebas)
- [Documentación](#documentación)
- [Forma de trabajo en Git](#forma-de-trabajo-en-git)
- [Problemas frecuentes](#problemas-frecuentes)

## Integrantes

| Nombre | Usuario GitHub |
|---|---|
| Vanessa Henao Gomez |vanessahg07 |

## Estado actual

Los cuatro cortes están implementados en código y pasan sus 263 pruebas. Falta lo que depende de cuentas o aprobaciones externas:

| Pendiente | Detalle |
|---|---|
| Publicar el despliegue | El `Dockerfile` y el blueprint `render.yaml` están listos y verificados localmente; falta crear el servicio con la cuenta de Render |
| Probar correo e IA reales | Funcionan con sus alternativas locales (memoria y simulado); falta configurar credenciales SMTP y un proveedor de IA |
| Aprobación de las reglas propias | RP-01, RP-02 y RP-03 están implementadas y probadas; falta la aprobación del docente |

## División de las entregas

| Corte | Alcance | Estado | Etiqueta |
|---|---|---|---|
| **1** | Ficha del Alojamiento, lenguaje ubicuo, modelo del negocio, casos de uso y **dominio con sus reglas invariantes y pruebas unitarias**; diseño de la API | Terminado | `entrega-1` |
| **2** | Usuarios y seguridad (JWT y roles), persistencia JPA, alojamiento, apartamentos, bloqueos, temporadas y tarifas, disponibilidad, cotización, reservas y folio **operando por API** | Terminado en código | `entrega-2` |
| **3** | Interfaz web en React: búsqueda, detalle, reserva, registro y salida, estado operativo, folio, cancelaciones y administración | Terminado en código | `entrega-3` |
| **4** | Canal externo con conflictos e idempotencia, notificaciones por correo, IA no bloqueante, imágenes, reportes, recordatorios automáticos y **sistema desplegado** | Terminado en código · despliegue pendiente | `entrega-4` |

Cada entrega se marca con su etiqueta: con `git checkout entrega-N` se ve el proyecto tal como quedó en ese corte.

## Qué hace el sistema

| Rol | Puede |
|---|---|
| **Huésped** | Buscar apartamentos por fechas y grupo, ver el detalle con galería y calendario, cotizar con desglose noche por noche, reservar, cancelar y consultar su folio |
| **Recepcionista** | Crear reservas directas, modificar, confirmar, registrar pagos, llegadas y salidas, declarar no-show y ver la ocupación del día |
| **Personal de servicio** | Ver los apartamentos por preparar, avanzar su preparación y registrar novedades (con sugerencia de gravedad por IA) |
| **Administrador** | Gestionar el alojamiento, apartamentos, bloqueos, temporadas, tarifas, políticas de cancelación, usuarios, canales externos y conflictos, y consultar reportes |
| **Canal externo** | Consultar disponibilidad y crear o cancelar reservas con su credencial, sin duplicar mensajes repetidos |

Reglas destacadas del negocio: no hay doble venta sin importar el canal (RN-01), la capacidad es un tope rígido (RN-02), el precio y la política quedan congelados al reservar (RN-22), el folio nunca borra movimientos (RN-16) y las reservas pendientes vencen solas a las 24 horas (RN-21). Reglas propias: estancia mínima en temporada alta (RP-01), descuento por estancia larga (RP-02) y mascotas con cargo (RP-03).

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security con JWT, Mail), Gradle 9.7 |
| Base de datos | H2 en memoria (desarrollo), H2 en archivo (perfil `prod`) o PostgreSQL (perfil `postgres`) |
| Frontend | React 19, TypeScript, Vite, Material UI 9, React Router y TanStack Query |
| Integraciones | SMTP, IA compatible con `/chat/completions` (Ollama, Groq…), Cloudinary; todas con alternativa local sin internet |
| Despliegue | Docker (una sola imagen con API y frontend) y Render |
| Pruebas de API | Colección [Bruno](https://www.usebruno.com/) en `bruno/` |

## Cómo ejecutar

### Requisitos

- **JDK 25.** En Windows: `winget install EclipseAdoptium.Temurin.25.JDK`, o descargarlo de [adoptium.net](https://adoptium.net) marcando *Set JAVA_HOME*.
- **Node.js 20 o superior** (solo para el frontend).
- No hace falta instalar Gradle: el proyecto incluye el wrapper (`gradlew`).

### 1. Backend

```bash
./gradlew bootRun        # Windows: .\gradlew.bat bootRun
```

- API: `http://localhost:9090/api`
- Consola H2: `http://localhost:9090/h2-console` — URL JDBC `jdbc:h2:mem:sgadb`, usuario `sa`, sin contraseña.

Al arrancar se cargan los datos de la Ficha del Alojamiento: 7 apartamentos con tarifas completas, temporadas, política de cancelación, un usuario por rol y reservas en los seis estados por los tres canales. Las fechas se calculan desde el día actual, así que siempre hay una llegada y una salida "de hoy".

### 2. Frontend

Con el backend corriendo, en otra terminal:

```bash
cd frontend
npm install
npm run dev              # http://localhost:5173
```

### 3. Todo junto con Docker

```bash
docker build -t sga-aldea-bahareque .
docker run -p 8080:8080 -v sga-datos:/app/datos -e SGA_JWT_SECRETO=<32+ caracteres> sga-aldea-bahareque
```

La imagen sirve la API y el frontend en `http://localhost:8080` y guarda los datos en el volumen `sga-datos`.

### Usuarios de demostración

| Rol | Correo |
|---|---|
| Administrador | `admin@aldeabahareque.co` |
| Recepcionista | `recepcion@aldeabahareque.co` |
| Personal de servicio | `servicio@aldeabahareque.co` |
| Huésped | `laura.gomez@correo.com` |
| Huésped | `carlos.mejia@correo.com` |

La contraseña de todos es `Bahareque2026`, o la que se defina en `SGA_CONTRASENA_DEMO`.

### Colección de peticiones

Abrir la carpeta `bruno/` con [Bruno](https://www.usebruno.com/) (*Open Collection*), elegir el entorno `local` y ejecutar primero `01 Autenticacion`, que guarda el token de cada rol. La carpeta `06 Simulador canal externo` simula una agencia: reserva aceptada, mensaje repetido, conflicto, credencial inválida y cancelación.

## Configuración

Ningún secreto se versiona: todo se configura con variables de entorno y cada integración tiene una alternativa local que funciona sin internet.

| Variable | Uso | Si no se define |
|---|---|---|
| `SGA_JWT_SECRETO` | Clave de firma de los tokens (32+ caracteres) | Se genera una temporal en cada arranque |
| `SGA_CONTRASENA_DEMO` | Contraseña de los usuarios de demostración | `Bahareque2026` |
| `SGA_CREDENCIAL_CANAL_DEMO` | Credencial del canal de demostración ViajaEje | Se genera y se muestra en el log |
| `SGA_NOTIFICACIONES` | `memoria` o `smtp` | `memoria` |
| `SGA_IA_HABILITADA`, `SGA_IA_PROVEEDOR` | Sugerencia de gravedad por IA: `simulado` u `openai` | Habilitada, `simulado` |
| `SGA_IMAGENES` | `local` o `cloudinary` | `local` |

La lista completa (SMTP, IA, Cloudinary, PostgreSQL y despliegue) está en [docs/integraciones.md](docs/integraciones.md).

## Estructura del proyecto

Arquitectura hexagonal (puertos y adaptadores): las dependencias apuntan hacia el dominio, que es Java puro.

```
sga/
├── src/main/java/co/edu/uniquindio/sga/
│   ├── domain/            Reglas del negocio y puertos (sin Spring ni JPA)
│   ├── application/       Casos de uso: orquestan el dominio a través de sus puertos
│   └── infrastructure/
│       ├── persistencia/  Entidades JPA y adaptadores de los repositorios
│       ├── rest/          Controladores, DTOs y manejo de errores
│       ├── seguridad/     JWT, roles y cifrado de contraseñas
│       ├── notificacion/  Notificador en memoria y por correo SMTP
│       ├── ia/            Sugerencia de gravedad simulada o con un modelo externo
│       ├── imagenes/      Almacén de imágenes local o en Cloudinary
│       └── configuracion/ Beans del dominio, tareas programadas y datos de prueba
├── src/test/              Pruebas del dominio (AAA, fechas fijas) y de integración de la API
├── frontend/              Interfaz React (ver frontend/README.md)
├── bruno/                 Colección de peticiones y simulador del canal externo
├── docs/                  Documentación del proyecto y de las entregas
├── Dockerfile             Imagen de despliegue (API + frontend)
└── render.yaml            Blueprint de despliegue en Render
```

## Pruebas

```bash
./gradlew test           # Windows: .\gradlew.bat test
```

- **Dominio:** cada regla invariante (RN-01 a RN-22) y cada regla propia (RP-01 a RP-03) tiene casos que la cumplen y casos que la violan. No usan Spring ni base de datos, siguen el patrón AAA y usan fechas fijas.
- **API:** `ApiIntegracionTest` recorre la API real con seguridad y persistencia: roles, restricción por propietario, doble venta, anticipo, llegada y salida con folio.
- **Integraciones:** `IntegracionesApiTest` (canal externo, reportes, recordatorios, IA e imágenes) e `IaDeshabilitadaTest` (el sistema opera con la IA apagada). Nunca llaman servicios externos reales.

El reporte queda en `build/reports/tests/test/index.html`. Para compilar el frontend: `cd frontend` y `npm run build`.

## Documentación

| Documento | Contenido |
|---|---|
| [Ficha del Alojamiento](docs/ficha-alojamiento.md) | Inventario, temporadas, tarifas, parámetros, políticas y reglas propias |
| [Glosario](docs/glosario.md) | Lenguaje ubicuo del enunciado y términos propios |
| [Modelo del negocio](docs/modelo-negocio.md) | Paquetes, ciclos de vida, decisiones de diseño y trazabilidad de reglas |
| [Casos de uso](docs/casos-de-uso.md) | Casos por rol y el corte en que se exponen |
| [Referencia de la API](docs/api.md) | Endpoints, roles, parámetros y errores |
| [Integraciones y despliegue](docs/integraciones.md) | Canal externo, correo, IA, imágenes, reportes, recordatorios, variables de entorno y despliegue |
| [Entregas](docs/entregas) | Documentos entregados en cada corte |

## Forma de trabajo en Git

- `main` contiene siempre una versión que compila y pasa las pruebas.
- Cada tema se trabaja en su propia rama (`feature/...`) y se integra a `main` con un merge explícito (`--no-ff`), así el historial muestra cada rama.
- Los mensajes de commit van en español, en infinitivo y describen un solo cambio: `Agregar entidad Reserva`, `Agregar endpoints de reservas y folio`.
- Cada entrega se marca con su etiqueta (`entrega-1` a `entrega-4`).
- `.gitignore` excluye secretos (`.env`, `application-local.properties`), datos locales (`datos/`) y carpetas generadas (`build/`, `node_modules/`, `dist/`).

## Problemas frecuentes

**`Cannot find a Java installation on your machine … languageVersion=25`**
El equipo no tiene el JDK 25. Instálalo (ver [Requisitos](#requisitos)), cierra y vuelve a abrir la terminal, ejecuta `.\gradlew.bat --stop` y repite el comando.

**La primera ejecución tarda varios minutos**
Es normal: el wrapper descarga Gradle y las dependencias una sola vez.

**`'gradlew' no se reconoce como un comando`**
En PowerShell hay que escribir `.\gradlew.bat`, con el punto y la barra al inicio.

**El frontend muestra errores de conexión**
El backend debe estar corriendo en el puerto 9090: Vite reenvía `/api` a esa dirección.

**Las sesiones se cierran al reiniciar el backend**
Sin `SGA_JWT_SECRETO` se genera una clave nueva en cada arranque. Defínela para conservar las sesiones.

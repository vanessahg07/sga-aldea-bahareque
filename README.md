# SGA — Aldea Bahareque

![Java 25](https://img.shields.io/badge/Java-25-34533A)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-34533A)
![React 19](https://img.shields.io/badge/React-19-34533A)
![Pruebas](https://img.shields.io/badge/pruebas-271%20en%20verde-2F6B3E)
![Estado](https://img.shields.io/badge/cortes-1%20a%204%20·%20desplegado-C9A55A)

**SGA - Sistema de Gestión de Alojamiento** para *Aldea Bahareque*, siete apartamentos turísticos independientes en Filandia, Quindío, en el Paisaje Cultural Cafetero. El sistema gestiona inventario, tarifas por temporada, disponibilidad, reservas, operación diaria y folios, y vende por tres canales (portal, directo y agencias externas) sin vender nunca dos veces la misma noche.

Proyecto final de **Programación Avanzada** — Programa de Ingeniería de Sistemas y Computación, Universidad del Quindío, 2026-2.

| | |
|---|---|
| **Aplicación web** | https://sga-aldea-bahareque.vercel.app |
| **API REST** | https://sga-api-ip10.onrender.com/api |

> La API está en el plan gratuito de Render: después de 15 minutos sin uso se suspende y la primera petición tarda unos 2 minutos y medio mientras arranca. [Por qué pasa](sga/docs/despliegue.md#la-primera-petición-tarda-arranque-en-frío).

## Contenido

- [Integrantes](#integrantes)
- [Estado actual](#estado-actual)
- [Organización del repositorio](#organización-del-repositorio)
- [División de las entregas](#división-de-las-entregas)
- [Qué hace el sistema](#qué-hace-el-sistema)
- [Tecnologías](#tecnologías)
- [Cómo ejecutar](#cómo-ejecutar)
- [Configuración](#configuración)
- [Despliegue](#despliegue)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Pruebas](#pruebas)
- [Documentación](#documentación)
- [Forma de trabajo en Git](#forma-de-trabajo-en-git)
- [Problemas frecuentes](#problemas-frecuentes)

## Integrantes

| Nombre | Usuario GitHub |
|---|---|
| _Vanessa Henao Gomez_ |vanessahg07 |
| _Miguel Angel Marulanda Cruz_ |miguelxmaru |

## Estado actual

Los cuatro cortes están implementados, pasan sus 271 pruebas automáticas y el sistema está desplegado:

| Componente | Estado |
|---|---|
| Backend (API REST) | Desplegado en Render con Docker, perfil `postgres` |
| Base de datos | PostgreSQL gestionado de Render |
| Frontend | Desplegado en Vercel |
| Correo | Activo con Brevo (SMTP) |
| Sugerencia de gravedad con IA | Activa con OpenRouter (modelo gratuito) |
| Imágenes | Activas con Cloudinary |

Pendiente: la aprobación del docente de las reglas propias RP-01, RP-02 y RP-03, que ya están implementadas y probadas.

## Organización del repositorio

| Carpeta | Contenido |
|---|---|
| [`sga/`](sga) | **Sistema completo y vigente:** backend, frontend, colección Bruno, documentación y archivos de despliegue. Render construye desde `sga/` y Vercel desde `sga/frontend` |
| `src/`, `docs/`, `build.gradle`, `gradle/` (raíz) | Primera entrega (Corte 1) tal como se presentó: dominio con sus pruebas, diseño de la API (`docs/api/openapi.yaml`), libro de identificación, documento y diagramas de esa entrega |

Todos los comandos de este documento se ejecutan dentro de `sga/`. Detalle en [sga/docs/estructura-del-proyecto.md](sga/docs/estructura-del-proyecto.md).

## División de las entregas

| Corte | Alcance | Estado | Etiqueta |
|---|---|---|---|
| **1** | Ficha del Alojamiento, lenguaje ubicuo, modelo del negocio, casos de uso y **dominio con sus reglas invariantes y pruebas unitarias**; diseño de la API | Terminado | `entrega-1` |
| **2** | Usuarios y seguridad (JWT y roles), persistencia JPA, alojamiento, apartamentos, bloqueos, temporadas y tarifas, disponibilidad, cotización, reservas y folio **operando por API** | Terminado | `entrega-2` |
| **3** | Interfaz web en React: búsqueda, detalle, reserva, registro y salida, estado operativo, folio, cancelaciones y administración | Terminado | `entrega-3` |
| **4** | Canal externo con conflictos e idempotencia, notificaciones por correo, IA no bloqueante, imágenes, reportes, recordatorios automáticos y **sistema desplegado** | Terminado y desplegado | `entrega-4` |

Cada entrega se marca con su etiqueta: con `git checkout entrega-N` se ve el proyecto tal como quedó en ese corte.

## Qué hace el sistema

| Rol | Puede |
|---|---|
| **Huésped** | Buscar apartamentos por fechas y grupo, ver el detalle con galería y calendario, cotizar con desglose noche por noche, reservar, cancelar, consultar su folio y recuperar su contraseña por correo |
| **Recepcionista** | Crear reservas directas, modificar, confirmar, registrar pagos, llegadas y salidas, declarar no-show y ver la ocupación del día |
| **Personal de servicio** | Ver los apartamentos por preparar, avanzar su preparación y registrar novedades (con sugerencia de gravedad por IA) |
| **Administrador** | Gestionar el alojamiento, apartamentos e imágenes, bloqueos, temporadas, tarifas, políticas de cancelación, usuarios, canales externos y conflictos, y consultar reportes y recordatorios |
| **Canal externo** | Consultar disponibilidad y crear o cancelar reservas con su credencial, sin duplicar mensajes repetidos |

Reglas destacadas del negocio: no hay doble venta sin importar el canal (RN-01), la capacidad es un tope rígido (RN-02), el precio y la política quedan congelados al reservar (RN-22), el folio nunca borra movimientos (RN-16) y las reservas pendientes vencen solas a las 24 horas (RN-21). Reglas propias: estancia mínima en temporada alta (RP-01), descuento por estancia larga (RP-02) y mascotas con cargo (RP-03).

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security con JWT, Mail), Gradle 9.7 |
| Base de datos | H2 en memoria (desarrollo), H2 en archivo (perfil `prod`) o PostgreSQL (perfil `postgres`, despliegue) |
| Frontend | React 19, TypeScript, Vite, Material UI 9, React Router y TanStack Query |
| Integraciones | Correo SMTP (Brevo), IA compatible con `/chat/completions` (OpenRouter, Ollama, Groq) y Cloudinary; todas con alternativa local sin internet |
| Despliegue | Backend en Render (Docker) con PostgreSQL de Render; frontend en Vercel |
| Pruebas de API | Colección [Bruno](https://www.usebruno.com/) en `sga/bruno/` |

El porqué de cada decisión está en [docs/decisiones-tecnicas.md](sga/docs/decisiones-tecnicas.md).

## Cómo ejecutar

### Opción rápida: el sistema desplegado

Abrir https://sga-aldea-bahareque.vercel.app. Si la API estaba suspendida, la primera carga tarda unos 2 minutos.

### Requisitos para ejecutarlo en local

- **JDK 25.** En Windows: `winget install EclipseAdoptium.Temurin.25.JDK`, o descargarlo de [adoptium.net](https://adoptium.net) marcando *Set JAVA_HOME*.
- **Node.js 20 o superior** (solo para el frontend).
- No hace falta instalar Gradle ni una base de datos: el proyecto incluye el wrapper (`gradlew`) y en desarrollo usa H2 en memoria.

### 1. Backend

```bash
cd sga
./gradlew bootRun        # Windows: .\gradlew.bat bootRun
```

- API: `http://localhost:9090/api`
- Consola H2: `http://localhost:9090/h2-console` — URL JDBC `jdbc:h2:mem:sgadb`, usuario `sa`, sin contraseña.

Al arrancar se cargan los datos de la Ficha del Alojamiento: 7 apartamentos con tarifas completas, temporadas, política de cancelación, un usuario por rol y reservas en los seis estados por los tres canales. Las fechas se calculan desde el día actual, así que siempre hay una llegada y una salida "de hoy".

En desarrollo la base es **en memoria**: al detener el backend se borran los cambios y el siguiente arranque vuelve a los datos de demostración. Es intencional (ver [por qué](sga/docs/despliegue.md#por-qué-h2-en-local-y-postgresql-en-el-despliegue)).

### 2. Frontend

Con el backend corriendo, en otra terminal:

```bash
cd sga/frontend
npm install
npm run dev              # http://localhost:5173
```

### Usuarios de demostración

| Rol | Correo |
|---|---|
| Administrador | `admin@aldeabahareque.co` |
| Recepcionista | `recepcion@aldeabahareque.co` |
| Personal de servicio | `servicio@aldeabahareque.co` |
| Huésped | `laura.gomez@correo.com` |
| Huésped | `carlos.mejia@correo.com` |

En local, la contraseña de todos es `Bahareque2026`. En el despliegue es la definida en `SGA_CONTRASENA_DEMO` en Render, que no se publica en el repositorio.

### Colección de peticiones

Abrir la carpeta `sga/bruno/` con [Bruno](https://www.usebruno.com/) (*Open Collection*), elegir el entorno `local` y ejecutar primero `01 Autenticacion`, que guarda el token de cada rol. La carpeta `06 Simulador canal externo` simula una agencia: reserva aceptada, mensaje repetido, conflicto, credencial inválida y cancelación.

## Configuración

Ningún secreto se versiona: todo se configura con variables de entorno y cada integración tiene una alternativa local que funciona sin internet.

| Variable | Uso | Si no se define |
|---|---|---|
| `SGA_JWT_SECRETO` | Clave de firma de los tokens (32+ caracteres) | En desarrollo se genera una temporal; en el despliegue la aplicación no arranca |
| `SGA_JWT_MINUTOS_EXPIRACION` | Vigencia del token de sesión | `60` |
| `SGA_CORS_ORIGENES` | Dominio del frontend desplegado (Vercel), separados por coma | Solo `localhost` |
| `SGA_CONTRASENA_DEMO` | Contraseña de los usuarios de demostración | `Bahareque2026` |
| `SGA_CREDENCIAL_CANAL_DEMO` | Credencial del canal de demostración ViajaEje | Se genera y se muestra en el log |
| `SGA_NOTIFICACIONES` y `SGA_SMTP_*` | `memoria` o `smtp`, y los datos del servidor de correo | `memoria` |
| `SGA_IA_HABILITADA`, `SGA_IA_PROVEEDOR`, `SGA_IA_URL`, `SGA_IA_MODELO`, `SGA_IA_CLAVE`, `SGA_IA_TIMEOUT_MS` | Sugerencia de gravedad: `simulado` u `openai` (cualquier API `/chat/completions`) | Habilitada, `simulado`, 5 s |
| `SGA_IMAGENES`, `CLOUDINARY_URL` | `local` o `cloudinary`, y las credenciales de Cloudinary | `local` |
| `SGA_BD_HOST`, `SGA_BD_PUERTO`, `SGA_BD_NOMBRE`, `SGA_BD_USUARIO`, `SGA_BD_CLAVE` | Conexión a PostgreSQL (perfil `postgres`); o la URL JDBC completa en `SGA_BD_URL` | — |
| `VITE_API_URL` (frontend) | Dominio del backend desplegado | Vacío: en desarrollo Vite reenvía `/api` al puerto 9090 |

La lista completa está en [docs/integraciones.md](sga/docs/integraciones.md#variables-de-entorno) y los valores usados en el despliegue, en [docs/despliegue.md](sga/docs/despliegue.md#variables-de-entorno-en-render).

## Despliegue

```
Navegador ──► Vercel (frontend React) ──► Render (API Spring Boot, perfil postgres) ──► PostgreSQL de Render
                                                 ├──► Cloudinary (imágenes)
                                                 ├──► Brevo (correo SMTP)
                                                 └──► OpenRouter (IA)
```

- **Render:** `sga/render.yaml` (Blueprint) crea la base `sga-bd` y el servicio `sga-api`, construido con `sga/Dockerfile`. Render conecta la base y genera la clave del JWT; los demás secretos se escriben en su panel.
- **Vercel:** usa `sga/frontend` como directorio raíz, con `VITE_API_URL` apuntando a Render.
- **Base de datos:** H2 en local para trabajar sin instalar nada; PostgreSQL en el despliegue porque el disco de Render no es persistente.

La guía completa, con los pasos, las variables, el arranque en frío, los límites del plan gratuito y los casos de verificación, está en [docs/despliegue.md](sga/docs/despliegue.md).

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
│       ├── seguridad/     JWT, roles, CORS y cifrado de contraseñas
│       ├── notificacion/  Notificador en memoria y por correo SMTP
│       ├── ia/            Sugerencia de gravedad simulada o con un modelo externo
│       ├── imagenes/      Almacén de imágenes local o en Cloudinary
│       └── configuracion/ Propiedades, beans del dominio, tareas programadas y datos de prueba
├── src/main/resources/    Configuración por perfil (desarrollo, prod y postgres)
├── src/test/              Pruebas del dominio (AAA, fechas fijas) y de integración de la API
├── frontend/              Interfaz React (ver sga/frontend/README.md)
├── bruno/                 Colección de peticiones y simulador del canal externo
├── docs/                  Documentación de soporte
├── Dockerfile             Imagen del backend para Render
└── render.yaml            Blueprint de Render: PostgreSQL y servicio web del backend
```

Detalle por paquete, del frontend y de la colección Bruno en [docs/estructura-del-proyecto.md](sga/docs/estructura-del-proyecto.md).

## Pruebas

```bash
cd sga
./gradlew test           # Windows: .\gradlew.bat test
```

- **Dominio:** cada regla invariante (RN-01 a RN-22) y cada regla propia (RP-01 a RP-03) tiene casos que la cumplen y casos que la violan. No usan Spring ni base de datos, siguen el patrón AAA y usan fechas fijas.
- **API:** `ApiIntegracionTest` recorre la API real con seguridad y persistencia: roles, restricción por propietario, doble venta, anticipo, llegada y salida con folio.
- **Integraciones:** `IntegracionesApiTest` (canal externo, reportes, recordatorios, IA e imágenes) e `IaDeshabilitadaTest` (el sistema opera con la IA apagada). Nunca llaman servicios externos reales.
- **Seguridad y configuración:** `ClaveJwtTest` (clave obligatoria en el despliegue) y `CredencialesCloudinaryTest` (lectura de `CLOUDINARY_URL`).

El reporte queda en `sga/build/reports/tests/test/index.html`. Para compilar el frontend: `cd sga/frontend` y `npm run build`.

## Documentación

| Documento | Contenido |
|---|---|
| [Ficha del Alojamiento](sga/docs/ficha-alojamiento.md) | Inventario, temporadas, tarifas, parámetros, políticas y reglas propias |
| [Glosario](sga/docs/glosario.md) | Lenguaje ubicuo del enunciado y términos propios |
| [Modelo del negocio](sga/docs/modelo-negocio.md) | Paquetes, ciclos de vida, decisiones de diseño y trazabilidad de reglas |
| [Casos de uso](sga/docs/casos-de-uso.md) | Casos por rol y el corte en que se exponen |
| [Referencia de la API](sga/docs/api.md) | Endpoints, roles, parámetros y errores |
| [Integraciones](sga/docs/integraciones.md) | Canal externo, correo, IA, imágenes, reportes, recordatorios y variables de entorno |
| [Decisiones técnicas](sga/docs/decisiones-tecnicas.md) | Justificación de la arquitectura, las tecnologías, la seguridad y la infraestructura |
| [Despliegue](sga/docs/despliegue.md) | Render, Vercel, H2 frente a PostgreSQL, arranque en frío, límites y verificación |
| [Solución de problemas](sga/docs/solucion-de-problemas.md) | Errores frecuentes en local, en el despliegue y en las integraciones |
| [Estructura del proyecto](sga/docs/estructura-del-proyecto.md) | Organización del repositorio, del backend, del frontend y de la documentación |
| [Diagramas](sga/docs/diagramas) | Diagrama de clases y mapa de agregados |
| [Entregas](sga/docs/entregas) | Documentos entregados en cada corte |

## Forma de trabajo en Git

- `main` contiene siempre una versión que compila y pasa las pruebas.
- Cada tema se trabaja en su propia rama (`feature/...`) y se integra a `main` con un merge explícito (`--no-ff`), así el historial muestra cada rama.
- Los mensajes de commit van en español, en infinitivo y describen un solo cambio: `Agregar entidad Reserva`, `Agregar endpoints de reservas y folio`.
- Cada entrega se marca con su etiqueta (`entrega-1` a `entrega-4`).
- `.gitignore` excluye secretos (`.env`, `application-local.properties`), datos locales (`datos/`) y carpetas generadas (`build/`, `node_modules/`, `dist/`).

## Problemas frecuentes

**`Cannot find a Java installation on your machine … languageVersion=25`**
El equipo no tiene el JDK 25. Instálalo (ver [Requisitos](#requisitos-para-ejecutarlo-en-local)), cierra y vuelve a abrir la terminal, ejecuta `.\gradlew.bat --stop` y repite el comando.

**`'gradlew' no se reconoce como un comando`**
En PowerShell hay que escribir `.\gradlew.bat`, con el punto y la barra al inicio.

**Los datos desaparecen al reiniciar el backend en local**
Es lo esperado con H2 en memoria. Para conservarlos, usar el perfil `prod`.

**El frontend local muestra errores de conexión**
El backend debe estar corriendo en el puerto 9090: Vite reenvía `/api` a esa dirección.

**La aplicación desplegada tarda en responder o muestra un error de conexión**
La API de Render estaba suspendida y está arrancando. Esperar unos 2 minutos y repetir. No se pierden datos.

**La sesión se cierra sola**
Los tokens duran 60 minutos. Volver a iniciar sesión.

Más casos (Render, Vercel, CORS, correo, IA, imágenes) en [docs/solucion-de-problemas.md](sga/docs/solucion-de-problemas.md).

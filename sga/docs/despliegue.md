# Despliegue — SGA Aldea Bahareque

El sistema está publicado en tres servicios gratuitos, cada uno con una responsabilidad:

| Parte | Servicio | Dirección |
|---|---|---|
| Frontend (React) | Vercel | https://sga-aldea-bahareque.vercel.app |
| API REST (Spring Boot) | Render, servicio web `sga-api` (Docker) | https://sga-api-ip10.onrender.com/api |
| Base de datos | Render, PostgreSQL `sga-bd` | Solo accesible desde la red interna de Render |

Comprobación rápida: https://sga-api-ip10.onrender.com/api/alojamiento devuelve la Ficha del Alojamiento en JSON.

## Arquitectura

```
                         ┌──────────────────────────── Render ───────────────────────────┐
Navegador ──► Vercel ──► │ sga-api (Docker, perfil postgres) ──► sga-bd (PostgreSQL)     │
           (HTML, JS,    └──────┬──────────────┬──────────────────┬─────────────────────┘
            CSS estáticos)      │              │                  │
                                ▼              ▼                  ▼
                           Cloudinary     Brevo (SMTP)      OpenRouter (IA)
                           (imágenes)     (correos)         (gravedad de novedades)
```

- El navegador descarga el frontend desde Vercel y, desde ahí, llama directamente a la API de Render (`VITE_API_URL`). Por eso la API habilita CORS solo para el dominio de Vercel (`SGA_CORS_ORIGENES`).
- La API es la única que habla con la base de datos y con los servicios externos. Las credenciales nunca llegan al navegador.
- Cada integración externa tiene una alternativa local y un tiempo máximo de espera: si un servicio externo falla, la operación principal continúa (ver [integraciones.md](integraciones.md)).

## Por qué H2 en local y PostgreSQL en el despliegue

El código de persistencia es el mismo en ambos casos: entidades JPA y repositorios de Spring Data detrás de los puertos del dominio. Lo único que cambia es la conexión, y se elige con el **perfil de Spring**:

| Perfil | Base de datos | Se usa en | Los datos |
|---|---|---|---|
| (por defecto) | H2 en memoria (`jdbc:h2:mem:sgadb`) | Desarrollo y pruebas automáticas | Se borran al detener el backend |
| `prod` | H2 en archivo (`SGA_DATOS`) | Ejecución local que conserva datos | Persisten en la carpeta `datos/` |
| `postgres` | PostgreSQL | **Render** | Persisten en la base gestionada |

**H2 en desarrollo** porque:

- No hay que instalar ni configurar nada: `gradlew bootRun` funciona en cualquier equipo con el JDK.
- Cada arranque empieza con la base limpia y los datos de demostración, con fechas calculadas desde el día actual. Las pruebas y las demostraciones son repetibles.
- Arranca en pocos segundos y trae una consola web (`/h2-console`) para revisar las tablas.

**PostgreSQL en el despliegue** porque:

- El disco de un servicio gratuito de Render **no es persistente**: se reinicia en cada despliegue y cada vez que el servicio se suspende. Una base H2, en memoria o en archivo, perdería todos los datos.
- La base gestionada de Render vive aparte del servicio web: los datos sobreviven a reinicios, suspensiones y nuevos despliegues.
- Es un motor de producción real: transacciones concurrentes, bloqueos (`PESSIMISTIC_WRITE` en la verificación de disponibilidad) y restricciones únicas (idempotencia del canal externo) con el comportamiento esperado.

Cambiar de base es **solo configuración**: `SPRING_PROFILES_ACTIVE=postgres` y las variables `SGA_BD_*`. El dominio, los casos de uso y los controladores no cambian. Esta es una de las ventajas de la arquitectura hexagonal: la base de datos es un adaptador.

Hibernate crea y actualiza las tablas al arrancar (`spring.jpa.hibernate.ddl-auto=update`). Los datos de demostración solo se cargan si la base está vacía, así que los reinicios no duplican ni borran información.

## La primera petición tarda (arranque en frío)

En el plan gratuito, Render **suspende el servicio web tras 15 minutos sin peticiones**. La siguiente petición lo despierta, y Render la retiene hasta que la aplicación está lista. En los registros del servicio se ve así:

```
23:18:40  Commencing graceful shutdown              ← se suspende por inactividad
23:32:41  Starting SgaApplication                    ← llega una petición y lo despierta
23:33:40  Database info: ... PostgreSQL 18.6         ← conexión con la base
23:34:48  Started SgaApplication in 134.401 seconds  ← listo para responder
```

El arranque tarda alrededor de **2 minutos y medio** porque:

1. Render vuelve a crear el contenedor desde la imagen Docker.
2. La JVM inicia con los recursos del plan gratuito: una fracción de CPU y 512 MB de memoria.
3. Spring Boot construye el contexto completo: 14 repositorios JPA, Hibernate valida y actualiza el esquema en PostgreSQL, se configuran la seguridad, las integraciones y las tareas programadas.

Una vez despierto, el servicio responde con normalidad hasta que vuelva a pasar un rato sin uso.

**Consecuencias y cómo manejarlas:**

- La primera acción después de un rato sin uso puede tardar o mostrar un error de conexión en el frontend. No se pierde nada: basta con esperar a que la API despierte y repetir.
- **Antes de una demostración**, abrir https://sga-api-ip10.onrender.com/api/alojamiento unos 3 minutos antes y esperar a que responda.
- Los datos **no** se pierden al suspenderse: están en PostgreSQL, que no se suspende.
- Las tareas programadas (vencimiento de reservas pendientes, recordatorios de las 8:00) solo corren mientras el servicio está despierto. Al despertar, la tarea de vencimiento se pone al día en su siguiente ejecución.
- Se evita con un plan de pago, que no suspende el servicio. No es necesario para el proyecto.

El frontend no tiene este problema: Vercel sirve archivos estáticos desde su CDN y responde de inmediato aunque la API esté suspendida.

## Por qué Render, Vercel y Docker

| Decisión | Motivo |
|---|---|
| **Render para la API y la base** | Plan gratuito para un servicio web y una base PostgreSQL. Con el *Blueprint* (`render.yaml`), toda la infraestructura queda descrita en el repositorio, y Render conecta la base con el servicio sin copiar credenciales a mano. |
| **Docker** | Render no ofrece Java 25 como entorno nativo. La imagen fija la versión exacta del JDK y el proceso de compilación, y es la misma que se puede probar en local. |
| **Vercel para el frontend** | Está pensado para aplicaciones estáticas de Vite: compila en cada push, sirve desde un CDN, no se suspende y da HTTPS automático. |
| **Frontend y backend separados** | Cada parte se despliega y escala por su cuenta, y el frontend sigue disponible mientras la API arranca. El costo es configurar CORS y la URL de la API. |

## Archivos de despliegue

| Archivo | Qué hace |
|---|---|
| `sga/Dockerfile` | Imagen del backend en dos etapas. La primera compila con `eclipse-temurin:25-jdk` (`gradlew bootJar`, sin pruebas: se ejecutan en desarrollo). La segunda ejecuta el JAR con `eclipse-temurin:25-jre`, con un usuario sin privilegios, perfil `postgres` y `-XX:MaxRAMPercentage=75` para ajustarse a la memoria del plan. |
| `sga/.dockerignore` | Deja fuera de la imagen el frontend, la colección Bruno, la documentación y las carpetas generadas. |
| `sga/render.yaml` | Blueprint: crea `sga-bd` y `sga-api`, conecta la base por `fromDatabase`, genera `SGA_JWT_SECRETO` y declara las demás variables. Como el proyecto vive en la carpeta `sga/` del repositorio, usa `dockerContext: ./sga` y `dockerfilePath: ./sga/Dockerfile`. |
| `sga/src/main/resources/application-postgres.properties` | Conexión a PostgreSQL desde las variables `SGA_BD_*` (o `SGA_BD_URL`), pool de 5 conexiones (la base gratuita admite pocas), consola H2 apagada y clave JWT obligatoria. |
| `sga/frontend/vercel.json` | Compilación con Vite (`npm ci`, `npm run build`, carpeta `dist`) y redirección de todas las rutas a `index.html`, para que funcionen las rutas de React al recargar la página. |

## Variables de entorno en Render

Las que tienen valor fijo están en `render.yaml`. Las marcadas con `sync: false` son secretos o datos de la cuenta: se escriben **solo en el panel de Render** y nunca se versionan.

| Variable | Origen | Valor en el despliegue |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `render.yaml` | `postgres` |
| `SGA_BD_HOST`, `SGA_BD_PUERTO`, `SGA_BD_NOMBRE`, `SGA_BD_USUARIO`, `SGA_BD_CLAVE` | Render, desde `sga-bd` | Automático |
| `SGA_JWT_SECRETO` | Render lo genera | Automático (nunca se ve en el repositorio) |
| `SGA_CORS_ORIGENES` | Panel | `https://sga-aldea-bahareque.vercel.app` |
| `SGA_CONTRASENA_DEMO` | Panel | Contraseña de los usuarios de demostración (no se publica) |
| `SGA_CREDENCIAL_CANAL_DEMO` | Panel | Credencial del canal de demostración (opcional) |
| `SGA_IMAGENES` | `render.yaml` | `cloudinary` |
| `CLOUDINARY_URL` | Panel | `cloudinary://<clave>:<secreto>@<nube>` |
| `SGA_NOTIFICACIONES`, `SGA_SMTP_HOST`, `SGA_SMTP_PUERTO` | `render.yaml` | `smtp`, `smtp-relay.brevo.com`, `2525` |
| `SGA_SMTP_USUARIO`, `SGA_SMTP_CLAVE`, `SGA_SMTP_REMITENTE` | Panel | Usuario y clave SMTP de Brevo, remitente verificado |
| `SGA_IA_PROVEEDOR`, `SGA_IA_URL`, `SGA_IA_TIMEOUT_MS` | `render.yaml` | `openai`, `https://openrouter.ai/api/v1`, `15000` |
| `SGA_IA_MODELO`, `SGA_IA_CLAVE` | Panel | `google/gemma-4-31b-it:free` y la clave de OpenRouter |
| `PORT` | Render | Render lo asigna (en los registros aparece el puerto 10000) |

En Vercel solo hay una variable: `VITE_API_URL=https://sga-api-ip10.onrender.com` (sin barra final y sin `/api`). Vite la incrusta **al compilar**: si cambia, hay que volver a desplegar el frontend.

## Pasos para desplegar desde cero

### 1. Backend y base de datos en Render

1. Subir el repositorio a GitHub.
2. En Render: **New → Blueprint**, elegir el repositorio y, como ruta del blueprint, `sga/render.yaml`.
3. En la revisión del Blueprint, completar las variables `sync: false` (tabla anterior). `SGA_CORS_ORIGENES` puede quedar con el dominio previsto de Vercel y corregirse después.
4. Aplicar. Render crea `sga-bd`, construye la imagen y arranca `sga-api`. El primer despliegue tarda varios minutos (descarga de Gradle y dependencias dentro de la imagen).
5. En **sga-api → Logs**, esperar `Started SgaApplication` y comprobar `/api/alojamiento`.

### 2. Frontend en Vercel

1. **Add New → Project**, importar el repositorio.
2. **Root Directory:** `sga/frontend`. El framework (Vite) y los comandos los toma de `vercel.json`.
3. **Environment Variables:** `VITE_API_URL` con la URL de Render; `VITE_MAPBOX_TOKEN` es opcional.
4. **Deploy.** Si el dominio asignado no es el previsto, actualizar `SGA_CORS_ORIGENES` en Render.

### 3. Actualizaciones

- Cada `git push` a `main` vuelve a desplegar la API (Render) y el frontend (Vercel).
- Si cambia `render.yaml`, hacer **Manual sync** en el Blueprint para que Render lea los cambios.
- Un valor fijo de `render.yaml` reemplaza lo que se haya escrito en el panel en el siguiente sync. Por eso los valores que se administran en el panel se declaran con `sync: false`.

## Casos de verificación del despliegue

| # | Caso | Cómo se comprueba | Resultado esperado |
|---|---|---|---|
| D-01 | La API responde | Abrir `/api/alojamiento` | JSON con la Ficha de Aldea Bahareque |
| D-02 | La API usa PostgreSQL | **sga-api → Logs** al arrancar | `The following 1 profile is active: "postgres"` y `Database version: 18.x` |
| D-03 | Los datos persisten | Crear una reserva, esperar a que el servicio se suspenda (15 min) y volver a consultarla | La reserva sigue ahí |
| D-04 | Frontend conectado | Abrir el dominio de Vercel y buscar apartamentos | Aparecen los 7 apartamentos con sus precios |
| D-05 | CORS | Usar la aplicación desde Vercel | Sin errores de CORS en la consola del navegador |
| D-06 | Rutas del frontend | Recargar la página en una ruta interna (por ejemplo, el detalle de un apartamento) | Carga la página, no un 404 |
| D-07 | Seguridad | Llamar `/api/reservas` sin token | 401 `NO_AUTENTICADO` |
| D-08 | Imágenes | Como administrador, subir una imagen a un apartamento | La URL devuelta empieza por `https://res.cloudinary.com/` |
| D-09 | Correo | "Olvidé mi contraseña" con un correo real | Llega el código (revisar también spam) |
| D-10 | IA | Registrar una novedad y pulsar *Sugerir* | Aparece la gravedad sugerida con su justificación |
| D-11 | IA caída | Con un modelo inexistente en `SGA_IA_MODELO` | Aviso "La sugerencia de IA no está disponible…" y la novedad se registra igual |
| D-12 | Arranque obligatorio con clave | Quitar `SGA_JWT_SECRETO` | La aplicación no arranca: "Falta SGA_JWT_SECRETO" |

## Límites del plan gratuito

| Límite | Efecto en el SGA |
|---|---|
| Render suspende el servicio web tras 15 minutos sin uso | Arranque en frío de unos 2 minutos y medio (ver arriba) |
| Disco del servicio web no persistente | Por eso la base es PostgreSQL gestionada y las imágenes van a Cloudinary |
| La base PostgreSQL gratuita tiene vencimiento y espacio limitado | Revisar la fecha de vencimiento en el panel de `sga-bd`; antes de que venza se puede crear otra base y volver a sincronizar el Blueprint (los datos de demostración se cargan solos) |
| Render bloquea los puertos SMTP 25, 465 y 587 en el plan gratuito | El correo sale por Brevo en el puerto **2525** |
| Los modelos `:free` de OpenRouter tienen un límite diario de peticiones y pueden retirarse | La IA es no bloqueante: si no responde, se elige la gravedad a mano. El modelo se cambia solo con `SGA_IA_MODELO` |
| Brevo gratuito permite un número limitado de correos al día | Suficiente para la demostración. Si un envío falla, queda en el registro y la operación continúa |

Errores frecuentes y su solución: [solucion-de-problemas.md](solucion-de-problemas.md).

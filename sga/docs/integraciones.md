# Integraciones y reportes — SGA Aldea Bahareque

Todas las integraciones siguen los requisitos comunes de la sección 10.3.3: están detrás de un puerto del dominio, tienen una alternativa local que funciona **sin internet y sin credenciales**, un tiempo de espera explícito y un comportamiento definido ante fallo. Cambiar de proveedor es **solo configuración** (variables de entorno); nunca requiere tocar código.

## Resumen por requisito

| Integración | Puerto (I-01) | Alternativa local (I-02) | Proveedor real (I-07) | Tiempo máximo (I-03) | Si falla (I-04, I-05) |
|---|---|---|---|---|---|
| Canal externo | `ProcesadorReservasExternas`, `CanalVentaRepositorio`, `BitacoraCanalRepositorio` | El simulador (colección Bruno) contra el servidor local | Contrato propio del curso | Lo controla el cliente del canal | El canal recibe 401/403/409/422 con un código de negocio; todo intento queda en la bitácora |
| Notificaciones | `Notificador` | `NotificadorEnMemoria` (log) | **Brevo** por SMTP en el despliegue (puerto 2525); en local también Gmail con clave de aplicación o un buzón de pruebas (Mailtrap) | 10 s de conexión, lectura y escritura | Se envía en segundo plano; el error queda en el log y la reserva o cancelación continúa |
| Modelo de IA | `SugeridorGravedad` | `SugeridorGravedadSimulado` (reglas locales) o IA deshabilitada | Cualquier API compatible con `/chat/completions`. En el despliegue, **OpenRouter** con un modelo gratuito; en local, Ollama o Groq | 5 s por defecto, 15 s en el despliegue (`SGA_IA_TIMEOUT_MS`, aplicado por `SugeridorConTiempoLimite`) | No aparece sugerencia y la interfaz lo dice; la persona elige la gravedad y registra la novedad igual |
| Imágenes | `AlmacenImagenes` | `AlmacenImagenesLocal` (carpeta del servidor) | **Cloudinary** (plan gratuito, sin tarjeta) | 15 s | El administrador ve el aviso (503) y puede pegar la URL de una imagen ya publicada |
| Mapa | (frontend) | Dirección y enlace a OpenStreetMap | Mapbox estático (`VITE_MAPBOX_TOKEN`) | Lo maneja el navegador | Se muestra la dirección sin mapa |

- **I-06:** las credenciales viven en variables de entorno (tabla al final) y nunca se versionan. `.env` y `application-local.properties` están en `.gitignore`.
- **I-08:** las pruebas automáticas usan siempre las alternativas locales: `IntegracionesApiTest` (canal, reportes, recordatorios, IA simulada e imágenes locales), `IaDeshabilitadaTest` (sistema con la IA apagada) y `SugeridorGravedadTest` (respuestas simuladas, modelo caído y tiempo límite).

## Integraciones activas en el despliegue

| Integración | Proveedor | Variables (valores secretos solo en el panel de Render) |
|---|---|---|
| Correo | Brevo, `smtp-relay.brevo.com:2525` | `SGA_NOTIFICACIONES=smtp`, `SGA_SMTP_HOST`, `SGA_SMTP_PUERTO`, `SGA_SMTP_USUARIO`, `SGA_SMTP_CLAVE`, `SGA_SMTP_REMITENTE` |
| IA | OpenRouter, modelo `google/gemma-4-31b-it:free` | `SGA_IA_PROVEEDOR=openai`, `SGA_IA_URL=https://openrouter.ai/api/v1`, `SGA_IA_MODELO`, `SGA_IA_CLAVE`, `SGA_IA_TIMEOUT_MS=15000` |
| Imágenes | Cloudinary | `SGA_IMAGENES=cloudinary`, `CLOUDINARY_URL` |
| Base de datos | PostgreSQL de Render | `SPRING_PROFILES_ACTIVE=postgres` y `SGA_BD_*` (las conecta el Blueprint) |

Cambiar cualquiera de estos proveedores por otro compatible, o por su alternativa local, es solo cambiar variables. El motivo de cada elección está en [decisiones-tecnicas.md](decisiones-tecnicas.md#dt-16--toda-integración-detrás-de-un-puerto-con-alternativa-local).

## Canal externo (10.3.1)

Contrato expuesto por el SGA. El canal se autentica con los encabezados `X-Canal` (código) y `X-Credencial-Canal`. La credencial la genera el administrador en *Administración → Canales de venta*: se guarda cifrada con BCrypt y solo se muestra una vez.

| Operación | Petición | Respuestas |
|---|---|---|
| Consultar disponibilidad | `GET /api/canal/disponibilidad?entrada&salida&personas` (o `fechasNacimiento=1990-01-01,2020-05-02`) | 200 con los apartamentos disponibles y su valor total |
| Crear reserva | `POST /api/canal/reservas` con `idExterno`, apartamento, fechas, hora de llegada, titular y acompañantes | **201 ACEPTADA** · **200 DUPLICADA** (RN-19) · **409 RECHAZADA_CONFLICTO** (RN-18) · 422 si viola otra regla |
| Cancelar reserva | `DELETE /api/canal/reservas/{idExterno}` | 200 CANCELADA · 200 YA_CANCELADA si el mensaje se repite · 404 si no existe |

- **Idempotencia (RN-19):** antes de crear se busca la reserva por (canal, idExterno); el bloqueo pesimista del apartamento serializa mensajes simultáneos y la restricción única `uk_reserva_referencia_externa` es la última garantía en la base de datos.
- **Conflictos (RN-18):** una reserva que choca con noches vendidas, un bloqueo o el tiempo de preparación se rechaza, se guarda como `ConflictoCanal` y la reserva existente no se toca. El administrador deja constancia de cómo lo resolvió (*Registrar revisión*).
- **Bitácora:** cada consulta, reserva, duplicado, conflicto, cancelación y credencial inválida queda en `evento_canal`, en una transacción propia para que se conserve aunque la operación se rechace.
- **Simulador:** carpeta `bruno/06 Simulador canal externo` con los escenarios aceptada, mensaje repetido, conflicto, credencial inválida, cancelación y cancelación repetida (cada petición trae sus pruebas). Antes de ejecutarla se asigna la variable secreta `credencialCanal` del entorno `local` con el valor de `SGA_CREDENCIAL_CANAL_DEMO`. Si esa variable no se configuró, el backend genera una credencial al arrancar y la muestra en el log.

## Integración de salida: notificaciones (Opción A)

Se notifican la recepción, la confirmación, la cancelación y el vencimiento de la reserva, las reservas externas, los recordatorios y el código de recuperación de contraseña.

```bash
# Ejemplo con Gmail y una clave de aplicación (https://myaccount.google.com/apppasswords)
SGA_NOTIFICACIONES=smtp
SGA_SMTP_HOST=smtp.gmail.com
SGA_SMTP_PUERTO=587
SGA_SMTP_USUARIO=cuenta@gmail.com
SGA_SMTP_CLAVE=<clave de aplicación>
```

En el despliegue se usa **Brevo**, porque el plan gratuito de Render bloquea los puertos SMTP 25, 465 y 587 y Brevo acepta el 2525:

```bash
SGA_NOTIFICACIONES=smtp
SGA_SMTP_HOST=smtp-relay.brevo.com
SGA_SMTP_PUERTO=2525
SGA_SMTP_USUARIO=<login SMTP de Brevo, termina en @smtp-brevo.com>
SGA_SMTP_CLAVE=<clave SMTP de Brevo>
SGA_SMTP_REMITENTE=<remitente verificado en Brevo>
```

Si `SGA_SMTP_REMITENTE` se omite, se usa `SGA_SMTP_USUARIO` como remitente.

## Modelo de IA (10.2): sugerencia de gravedad de novedades

Al registrar una novedad, el personal puede pedir una sugerencia de gravedad (baja, media, alta o crítica) con su justificación. La IA **solo sugiere**: la persona decide y la novedad se registra aunque la IA falle, tarde o esté apagada.

- **Demostrar el sistema sin IA:** `SGA_IA_HABILITADA=false`. El botón *Sugerir* queda deshabilitado con el aviso "La sugerencia con IA está deshabilitada".
- **Proveedor simulado** (por defecto, sin conexión): `SGA_IA_PROVEEDOR=simulado`.
- **Modelo local con Ollama:** `SGA_IA_PROVEEDOR=openai`, `SGA_IA_URL=http://localhost:11434/v1`, `SGA_IA_MODELO=llama3.2`.
- **Groq (nivel gratuito):** `SGA_IA_URL=https://api.groq.com/openai/v1`, `SGA_IA_MODELO=llama-3.1-8b-instant`, `SGA_IA_CLAVE=<clave>`.
- **OpenRouter (modelos gratuitos `:free`, usado en el despliegue):** `SGA_IA_URL=https://openrouter.ai/api/v1`, `SGA_IA_MODELO=google/gemma-4-31b-it:free`, `SGA_IA_CLAVE=<clave>`, `SGA_IA_TIMEOUT_MS=15000`. Para usar los modelos gratuitos hay que permitirlos en *Settings → Privacy* de OpenRouter. Conviene un modelo de instrucciones sin razonamiento: responde dentro del tiempo máximo.

El modelo recibe la descripción de la novedad y debe responder un JSON con `gravedad` (`BAJA`, `MEDIA`, `ALTA` o `CRITICA`) y `justificacion`. Si la respuesta no se puede interpretar, se trata como "sin sugerencia".

## Imágenes

`POST /api/imagenes` (multipart, campo `archivo`, solo administrador) acepta JPG, PNG o WEBP de hasta 5 MB y devuelve la URL. En la ficha del apartamento, *Subir archivo* agrega la imagen a la galería; se guarda con *Guardar imágenes*.

- **Local:** `SGA_IMAGENES=local` guarda en `./datos/imagenes` y publica en `/archivos/imagenes/...`.
- **Cloudinary (despliegue):** `SGA_IMAGENES=cloudinary` con `CLOUDINARY_URL=cloudinary://<clave>:<secreto>@<nube>`, tal como la muestra el panel de Cloudinary, o con `SGA_CLOUDINARY_NUBE`, `SGA_CLOUDINARY_CLAVE` y `SGA_CLOUDINARY_SECRETO` por separado (tienen prioridad). La subida es firmada: el secreto nunca sale del servidor.
- En Render se usa Cloudinary porque el disco del servicio gratuito no es persistente: una imagen guardada en local se perdería en el siguiente reinicio.

## Reportes (7.9) e indicadores propios (L-19)

Todos se filtran por periodo con **ambas fechas incluidas** y se descargan en CSV (funcionalidad opcional 8, separador `;` para Excel en español).

| Reporte | Definición |
|---|---|
| Ocupación | Noches vendidas / (apartamentos a la venta × noches del periodo). Cuentan las noches de reservas CONFIRMADA, EN_CURSO y FINALIZADA; las pendientes aún no están en firme y las canceladas y no-show liberaron sus noches (RN-12). |
| Ingresos | Suma de los **pagos** con fecha en el periodo, por medio de pago. Un reverso resta en el medio del pago original. Los cargos no pagados no son ingreso. |
| Reservas por canal | Reservas **creadas** en el periodo por canal y estado actual, incluidas canceladas y no-show. |
| Tarifa promedio por noche (propio) | Ingreso por alojamiento de las noches vendidas del periodo / noches vendidas. |
| Ingreso por noche disponible (propio) | Ingreso por alojamiento / noches disponibles. |
| Tasa de no-show (propio) | No-show / reservas en firme cuya entrada cae en el periodo (EN_CURSO, FINALIZADA o NO_SHOW). |
| Tasa de cancelación (propio) | Canceladas / reservas creadas en el periodo. |
| Antelación promedio (propio) | Días promedio entre la creación de la reserva y la entrada. |

Nota: el SGA no guarda el histórico de activaciones del inventario, así que la base de noches disponibles usa los apartamentos a la venta al momento de consultar.

## Funcionalidades opcionales (sección 11)

1. **Recordatorios automáticos (6):** cada día a las 8:00 (hora de Colombia) se recuerda la llegada a las reservas confirmadas que entran dentro de `diasRecordatorioLlegada` días (parámetro del alojamiento, 2 en la Ficha; 0 lo desactiva) y la salida el día anterior a las reservas en curso. Cada reserva recibe cada recordatorio una sola vez (restricción única por reserva y tipo). En *Administración → Recordatorios* se ve el historial y se pueden enviar a mano.
2. **Exportación de reportes (8):** CSV de cada reporte.

## Despliegue

La persistencia y las integraciones se cambian **solo con configuración**:

| Perfil | Base de datos | Activación |
|---|---|---|
| (por defecto) | H2 en memoria | Desarrollo y pruebas |
| `prod` | H2 en archivo (`SGA_DATOS`, por defecto `./datos`) | `SPRING_PROFILES_ACTIVE=prod` |
| `postgres` | PostgreSQL de Render (o cualquier PostgreSQL gestionado) | `SPRING_PROFILES_ACTIVE=postgres` + `SGA_BD_HOST`, `SGA_BD_PUERTO`, `SGA_BD_NOMBRE`, `SGA_BD_USUARIO`, `SGA_BD_CLAVE` (o `SGA_BD_URL`) |

En los perfiles `prod` y `postgres` la clave del JWT es obligatoria: sin `SGA_JWT_SECRETO` la aplicación no arranca.

La guía completa (arquitectura, pasos en Render y Vercel, por qué H2 en local y PostgreSQL en el despliegue, arranque en frío y casos de verificación) está en [despliegue.md](despliegue.md).

Para probar la imagen del backend en local contra un PostgreSQL:

```bash
docker build -t sga-api .
docker run -p 8080:8080 -e SGA_JWT_SECRETO=<32+ caracteres> -e SGA_BD_URL=jdbc:postgresql://<host>:5432/sga -e SGA_BD_USUARIO=<usuario> -e SGA_BD_CLAVE=<clave> sga-api
```

## Variables de entorno

| Variable | Uso | Por defecto |
|---|---|---|
| `SGA_JWT_SECRETO` | Clave de firma de los JWT (32+ caracteres) | Temporal en desarrollo; obligatoria en `prod` y `postgres` |
| `SGA_JWT_MINUTOS_EXPIRACION` | Vigencia del token de sesión | `60` |
| `SGA_CORS_ORIGENES` | Dominios del frontend desplegado, separados por coma | Solo `localhost` |
| `SGA_CONTRASENA_DEMO` | Contraseña de los usuarios de demostración | `Bahareque2026` |
| `SGA_CREDENCIAL_CANAL_DEMO` | Credencial del canal ViajaEje (16+ caracteres) | Generada y mostrada en el log |
| `SGA_NOTIFICACIONES` | `memoria` o `smtp` | `memoria` |
| `SGA_SMTP_HOST`, `SGA_SMTP_PUERTO`, `SGA_SMTP_USUARIO`, `SGA_SMTP_CLAVE`, `SGA_SMTP_REMITENTE` | Servidor de correo | — |
| `SGA_IA_HABILITADA` | `true` o `false` | `true` |
| `SGA_IA_PROVEEDOR` | `simulado` u `openai` | `simulado` |
| `SGA_IA_URL`, `SGA_IA_MODELO`, `SGA_IA_CLAVE` | API compatible con `/chat/completions` | Ollama local, `llama3.2` |
| `SGA_IA_TIMEOUT_MS` | Tiempo máximo de espera del modelo | `5000` |
| `SGA_IMAGENES` | `local` o `cloudinary` | `local` |
| `SGA_IMAGENES_DIRECTORIO` | Carpeta del almacén local | `./datos/imagenes` |
| `CLOUDINARY_URL` | Credenciales de Cloudinary en una sola variable (`cloudinary://clave:secreto@nube`) | — |
| `SGA_CLOUDINARY_NUBE`, `SGA_CLOUDINARY_CLAVE`, `SGA_CLOUDINARY_SECRETO` | Cuenta de Cloudinary por separado (tienen prioridad sobre `CLOUDINARY_URL`) | — |
| `SGA_FRONTEND_DIRECTORIO` | Frontend compilado servido por el backend (opcional) | — |
| `SGA_DATOS` | Carpeta de datos del perfil `prod` | `./datos` |
| `SGA_BD_HOST`, `SGA_BD_PUERTO`, `SGA_BD_NOMBRE`, `SGA_BD_USUARIO`, `SGA_BD_CLAVE` | PostgreSQL del perfil `postgres` | Puerto `5432` |
| `SGA_BD_URL` | URL JDBC completa (reemplaza host, puerto y nombre) | — |
| `VITE_API_URL` (frontend) | Dominio del backend desplegado | Vacío en desarrollo |
| `PORT` | Puerto del servidor | `9090` en desarrollo, `8080` en despliegue |

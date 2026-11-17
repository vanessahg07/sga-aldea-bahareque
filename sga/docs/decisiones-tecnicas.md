# Decisiones técnicas — SGA Aldea Bahareque

Este documento justifica las decisiones de arquitectura, tecnología e infraestructura del SGA. Las decisiones propias del modelo de negocio (congelamiento de precios, folio de solo inserción, tiempo de preparación…) están en [modelo-negocio.md](modelo-negocio.md#decisiones-de-diseño).

## Arquitectura

### DT-01 · Arquitectura hexagonal (puertos y adaptadores)

El código se organiza en tres capas y las dependencias apuntan siempre hacia el dominio:

| Capa | Paquete | Depende de |
|---|---|---|
| Dominio | `domain` | Nada: Java puro, sin Spring, JPA ni Jakarta |
| Aplicación | `application` | Dominio |
| Infraestructura | `infrastructure` | Aplicación y dominio |

**Por qué:** las reglas del negocio (RN-01 a RN-22, RP-01 a RP-03) son la parte más valiosa y más probada del sistema. Al aislarlas:

- Se prueban sin base de datos ni servidor, en milisegundos y con fechas fijas.
- Cambiar una tecnología no toca las reglas. Ya pasó en el proyecto: la base pasó de H2 a PostgreSQL, el correo de memoria a SMTP, la IA de simulada a OpenRouter y las imágenes de disco a Cloudinary, y en ningún caso cambió el dominio.

Los **puertos** son interfaces del dominio (`ReservaRepositorio`, `Notificador`, `SugeridorGravedad`, `AlmacenImagenes`, `Reloj`, `CifradorContrasenas`…). Los **adaptadores** las implementan en `infrastructure`.

### DT-02 · Casos de uso en servicios de aplicación

Cada servicio de `application` (`ReservaServicio`, `FolioServicio`, `ApartamentoServicio`…) representa un grupo de casos de uso: abre la transacción, carga los agregados por sus puertos, invoca al dominio y guarda. No contiene reglas de negocio; solo orquesta.

La verificación de "solo sus propias reservas" para el huésped vive aquí (`Solicitante.exigirAcceso`), porque depende de quién hace la petición y no de la reserva en sí.

### DT-03 · DTOs como `record` y traducción explícita

- Las solicitudes y respuestas de la API son `record` de Java, declarados junto a su controlador y validados con Jakarta Validation (`@NotNull`, `@Email`, `@Past`…).
- El dominio nunca se expone directamente en JSON: la clase `Respuestas` y los métodos de cada controlador traducen entre DTOs y objetos del dominio.
- No se usa MapStruct: la traducción es corta, explícita y fácil de seguir. Agregar una librería de generación de código no compensaba para este tamaño.

### DT-04 · Errores con código de negocio estable

- El dominio lanza `ExcepcionNegocio` con un `CodigoError` (por ejemplo, `NOCHES_NO_DISPONIBLES`).
- `ManejadorErrores` (`@RestControllerAdvice`) la convierte en una respuesta `ErrorRespuesta` con código, mensaje legible, detalles y fecha, y el HTTP adecuado (404, 409 o 422).
- La seguridad responde con el mismo formato (401 `NO_AUTENTICADO`, 403 `ACCESO_DENEGADO`).

**Por qué:** el frontend y el canal externo deciden con el código, no con el texto. Nunca se expone una traza técnica.

## Backend

### DT-05 · Java 25 y Spring Boot 4.1

- Java 25 es la versión LTS vigente. Permite `record`, `sealed`, *pattern matching* y `switch` de expresiones, que hacen el dominio más corto y seguro.
- Spring Boot 4.1 trae juntos todo lo que pide el proyecto: Web MVC, Data JPA, Validation, Security con servidor de recursos JWT, Mail y tareas programadas.
- Gradle con el wrapper: cualquier equipo compila con la misma versión, sin instalar Gradle.

### DT-06 · Persistencia con JPA, separada del dominio

- Cada agregado tiene su entidad JPA (`ReservaEntity`, `ApartamentoEntity`…) y un adaptador que traduce entre la entidad y el objeto del dominio.
- El dominio no tiene anotaciones de JPA. Los objetos del dominio tienen constructores privados y métodos de fábrica que validan; las entidades JPA son solo el formato de almacenamiento.
- **Concurrencia (RN-01):** al reservar se bloquea la fila del apartamento (`PESSIMISTIC_WRITE`). Dos reservas simultáneas para las mismas noches se procesan una después de la otra y la segunda se rechaza.
- **Idempotencia (RN-19):** además de la verificación en el caso de uso, una restricción única (canal, id externo) en la base es la última garantía contra duplicados.

### DT-07 · H2 en desarrollo, PostgreSQL en el despliegue

Se elige con perfiles de Spring y no cambia el código. Justificación completa en [despliegue.md](despliegue.md#por-qué-h2-en-local-y-postgresql-en-el-despliegue).

### DT-08 · Esquema generado por Hibernate

`ddl-auto=update` crea y ajusta las tablas al arrancar, tanto en H2 como en PostgreSQL.

**Por qué:** el modelo cambió en cada corte, y así el esquema siempre coincide con las entidades sin mantener scripts a mano.

**Limitación conocida:** `update` agrega tablas y columnas, pero no renombra ni borra. Para un sistema en producción real, el siguiente paso sería versionar el esquema con migraciones (Flyway).

### DT-09 · Dinero, fechas y zona horaria

- **Dinero:** `Dinero` envuelve `BigDecimal` en pesos colombianos con redondeo explícito. Nunca se usa `double` para valores monetarios.
- **Fechas:** `RangoFechas` es semiabierto `[entrada, salida)`, así la noche de salida queda libre para la siguiente reserva.
- **Hora actual:** el dominio no llama a `LocalDate.now()`. Recibe la fecha del puerto `Reloj`, configurado en `America/Bogota` (`sga.zona-horaria`). Las pruebas usan fechas fijas y el servidor de Render, en UTC, calcula los días en hora de Colombia.

### DT-10 · Tareas programadas

- `TareaVencimientoReservas` corre cada minuto y cancela las reservas pendientes vencidas (RN-21).
- `TareaRecordatorios` corre todos los días a las 8:00, hora de Colombia.

Ambas usan los mismos casos de uso que la API, así que la regla es una sola.

## Seguridad

### DT-11 · JWT sin estado

- El inicio de sesión devuelve un JWT firmado con HMAC-SHA256. Contiene el id del usuario, su nombre y su rol (`rol`), y lo valida el servidor de recursos de Spring Security.
- **Sin sesiones en el servidor:** la API funciona igual con una o varias instancias y después de reiniciarse, siempre que la clave sea la misma.
- **Clave en `SGA_JWT_SECRETO`:** mínimo 32 caracteres. En el despliegue es obligatoria y la aplicación no arranca sin ella. En desarrollo, si falta, se genera una temporal.
- **Emisor validado:** se valida el emisor (`iss = sga-aldea-bahareque`) además de la firma y la fecha de vencimiento.
- **Vigencia de 60 minutos** (`SGA_JWT_MINUTOS_EXPIRACION`): limita el daño de un token filtrado.

### DT-12 · Autorización por rol y por propietario

- `@PreAuthorize` en cada endpoint define qué roles pueden usarlo (`HUESPED`, `RECEPCIONISTA`, `ADMINISTRADOR`, `PERSONAL_SERVICIO`).
- Sobre esa base, el caso de uso verifica que un huésped solo acceda a sus propias reservas y folios.
- El personal de servicio no puede consultar ni cancelar reservas (403).

### DT-13 · Contraseñas y credenciales

- Las contraseñas de usuarios y las credenciales de los canales externos se guardan con **BCrypt**, detrás del puerto `CifradorContrasenas`.
- La credencial de un canal se muestra una sola vez al generarla.

### DT-14 · Secretos fuera del repositorio

- Toda clave (JWT, base de datos, Cloudinary, SMTP, IA) entra por variables de entorno.
- En Render se escriben en el panel (`sync: false` en `render.yaml`) o las genera Render (`generateValue`).
- `.env`, `.env.local` y `application-local.properties` están en `.gitignore`. La colección Bruno guarda la credencial del canal como variable secreta.

### DT-15 · CORS restringido

La API solo acepta peticiones del navegador desde `localhost` (desarrollo) y desde los dominios de `SGA_CORS_ORIGENES` (el dominio de Vercel). Expone `Content-Disposition` para que el navegador pueda leer el nombre de los CSV descargados.

## Integraciones

### DT-16 · Toda integración detrás de un puerto, con alternativa local

| Integración | Alternativa local | Proveedor en el despliegue | Por qué ese proveedor |
|---|---|---|---|
| Correo | `NotificadorEnMemoria` (registro) | **Brevo** por SMTP, puerto 2525 | Plan gratuito sin tarjeta y acepta el puerto 2525. Render gratuito bloquea los puertos SMTP habituales (25, 465 y 587), así que Gmail no funciona desde ahí |
| IA | `SugeridorGravedadSimulado` (reglas locales) | **OpenRouter**, modelo `google/gemma-4-31b-it:free` | API compatible con `/chat/completions`: el mismo adaptador sirve para Ollama, Groq u OpenRouter. Tiene modelos gratuitos y el modelo se cambia con una variable |
| Imágenes | `AlmacenImagenesLocal` (carpeta del servidor) | **Cloudinary** | El disco de Render no es persistente; Cloudinary guarda las imágenes y las sirve por CDN. Plan gratuito sin tarjeta |
| Mapa | Dirección y enlace a OpenStreetMap | Mapbox (opcional) | Solo visual; sin token el sistema funciona igual |

Reglas comunes:

- **Tiempo máximo explícito:** 10 s para el correo, 15 s para la IA en el despliegue y 15 s para las imágenes.
- **Las fallas nunca bloquean la operación principal.** El correo se envía en segundo plano. La IA solo sugiere y la persona decide. Si falla Cloudinary, se responde 503 con un mensaje y se puede pegar una URL.
- **Las pruebas automáticas nunca llaman servicios reales:** usan las alternativas locales.

Detalle en [integraciones.md](integraciones.md).

### DT-17 · La IA solo sugiere

La gravedad de una novedad la decide siempre la persona. El modelo propone un valor y una justificación, y la persona puede cambiarlo.

**Por qué:** un modelo puede equivocarse, tardar o no estar disponible, y la operación del alojamiento no puede depender de él. Además, la IA se puede apagar (`SGA_IA_HABILITADA=false`) para demostrar que el sistema opera sin ella.

## Frontend

### DT-18 · React, TypeScript y Vite

| Tecnología | Por qué |
|---|---|
| React 19 | Biblioteca de componentes estándar |
| TypeScript | Los tipos de la API (`api/tipos.ts`) detectan en compilación los errores de forma entre frontend y backend |
| Vite | Arranque y recarga instantáneos en desarrollo, y compilación a archivos estáticos que Vercel sirve directamente |

### DT-19 · Material UI y TanStack Query

- **Material UI 9:** componentes accesibles y consistentes (tablas, formularios, diálogos, calendario) con un tema propio de colores Bahareque.
- **TanStack Query:** cache y recarga de los datos de la API, estados de carga y error, e invalidación después de cada cambio. Evita escribir esa lógica a mano en cada página.
- **React Router:** rutas protegidas por rol.

### DT-20 · Las reglas viven en el backend

El frontend valida formatos para orientar a la persona (campos vacíos, fechas en orden), pero cualquier regla del negocio la decide la API, y su mensaje se muestra tal cual. Así no hay dos versiones de una misma regla que puedan contradecirse.

### DT-21 · URL de la API por entorno

- En desarrollo, Vite reenvía `/api` al backend local (puerto 9090) y no hace falta CORS.
- En el despliegue, `VITE_API_URL` apunta a Render y `urlRecurso()` completa las URLs relativas de los archivos (`/archivos/...`).

## Pruebas y calidad

### DT-22 · Pirámide de pruebas

| Nivel | Qué cubre | Herramientas |
|---|---|---|
| Dominio | Cada regla con casos que la cumplen y que la violan | JUnit 5, AssertJ, patrón AAA, fechas fijas, sin Spring |
| Integración de la API | Seguridad, roles, propietario, persistencia, flujos completos | `@SpringBootTest`, MockMvc, H2 en memoria |
| Integraciones | Canal externo, IA simulada y caída, imágenes locales, IA deshabilitada | Alternativas locales, sin red |
| Manual / exploratorio | Flujos de la API y simulador del canal externo | Colección Bruno |

Total: **271 pruebas automáticas** en verde (`gradlew test`).

### DT-23 · Bruno para las pruebas manuales

Las colecciones de Bruno son archivos de texto dentro del repositorio: se versionan, se revisan en los commits y no dependen de una cuenta en la nube. Los tokens por rol se guardan en variables del entorno y la credencial del canal, como variable secreta.

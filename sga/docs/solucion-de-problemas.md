# Solución de problemas — SGA Aldea Bahareque

Errores que pueden aparecer al ejecutar o desplegar el SGA, su causa y cómo resolverlos. Se agrupan por dónde se presentan.

**Dónde mirar primero:**

| Lugar | Qué muestra |
|---|---|
| **Render → sga-api → Logs** | Errores del servidor (líneas `ERROR` y `WARN`), arranques y suspensiones |
| **Navegador → F12 → Network** | Código HTTP y cuerpo de la respuesta de cada petición (`codigo` y `mensaje`) |
| **Navegador → F12 → Console** | Errores de CORS y de JavaScript |
| **Terminal de `gradlew bootRun`** | Errores del backend en local |

Un 4xx (401, 403, 409, 422) **no** aparece como error en los registros del servidor: es una respuesta normal de la API. Se ve en la pestaña Network.

## Ejecución local

**`Cannot find a Java installation on your machine … languageVersion=25`**
El equipo no tiene el JDK 25. Instalarlo (`winget install EclipseAdoptium.Temurin.25.JDK`), cerrar y abrir la terminal, ejecutar `.\gradlew.bat --stop` y repetir.

**`'gradlew' no se reconoce como un comando`**
En PowerShell se escribe `.\gradlew.bat`, con punto y barra al inicio.

**La primera ejecución tarda varios minutos**
El wrapper descarga Gradle y las dependencias una sola vez.

**`Port 9090 was already in use`**
Otro backend sigue abierto. Cerrarlo, o en PowerShell buscar el proceso con `netstat -ano | findstr :9090` y terminarlo con `taskkill /PID <pid> /F`.

**Los datos que creé desaparecieron al reiniciar el backend**
Es el comportamiento esperado: en desarrollo la base es H2 **en memoria** y se reinicia con los datos de demostración. Para conservarlos en local, usar el perfil `prod` (H2 en archivo): `SPRING_PROFILES_ACTIVE=prod` y `SGA_JWT_SECRETO` con 32 o más caracteres. Ver [despliegue.md](despliegue.md#por-qué-h2-en-local-y-postgresql-en-el-despliegue).

**Las sesiones se cierran al reiniciar el backend**
Sin `SGA_JWT_SECRETO` se genera una clave nueva en cada arranque y los tokens anteriores dejan de ser válidos. Definirla para conservarlas.

**El frontend local muestra errores de conexión**
El backend debe estar corriendo en el puerto 9090: Vite reenvía `/api` a esa dirección. En local, `VITE_API_URL` debe quedar vacía.

**No sé la credencial del canal de demostración**
Si `SGA_CREDENCIAL_CANAL_DEMO` no está definida, el backend genera una al arrancar y la muestra en el registro. Copiarla en la variable secreta `credencialCanal` del entorno de Bruno.

## Despliegue en Render (backend y base de datos)

**La primera petición tarda unos 2 minutos o el frontend muestra un error de conexión**
El servicio gratuito se suspende tras 15 minutos sin uso y tarda unos 2 minutos y medio en volver a arrancar. No es una falla y no se pierden datos. Esperar a que `/api/alojamiento` responda y repetir la acción. Explicación completa en [despliegue.md](despliegue.md#la-primera-petición-tarda-arranque-en-frío).

**`failed to read dockerfile: open Dockerfile: no such file or directory`**
Render busca el `Dockerfile` en la raíz del repositorio, pero el proyecto está en la carpeta `sga/`. `render.yaml` lo indica con `dockerContext: ./sga` y `dockerfilePath: ./sga/Dockerfile`. Si se cambió la estructura, ajustar esas rutas y hacer **Manual sync**.

**`Falta SGA_JWT_SECRETO: es obligatorio en el despliegue`**
El perfil `postgres` exige la clave del JWT. En el Blueprint la genera Render (`generateValue`). Si se borró, crear la variable en **sga-api → Environment** con 32 o más caracteres. Cambiarla cierra todas las sesiones abiertas.

**La aplicación no arranca y el registro menciona `SGA_BD_HOST` o la conexión a PostgreSQL**
El servicio no recibió los datos de la base. Comprobar que `sga-bd` está *Available* y que las variables `SGA_BD_*` existen en el servicio (las crea el Blueprint con `fromDatabase`). Hacer **Manual sync** si faltan.

**¿La API está usando H2 en lugar de PostgreSQL?**
En el registro de arranque deben aparecer `The following 1 profile is active: "postgres"` y `Database version: 18.x` con una URL `jdbc:postgresql://…`. Si aparece `jdbc:h2:mem`, falta `SPRING_PROFILES_ACTIVE=postgres`.

**Cambié una variable en el panel y después volvió a su valor anterior**
Las variables con valor fijo en `render.yaml` se reescriben en cada sincronización del Blueprint. Cambiarlas en `render.yaml` (y hacer commit), o declararlas con `sync: false` si se administran desde el panel.

**El primer despliegue tarda mucho**
La imagen descarga Gradle y todas las dependencias y compila el proyecto. Los siguientes despliegues reutilizan capas en caché cuando no cambian `build.gradle` ni el wrapper.

**La base de datos dejó de estar disponible**
La base gratuita de Render tiene vencimiento. Revisar el panel de `sga-bd`. Se puede crear una nueva y sincronizar el Blueprint; al arrancar con la base vacía se cargan otra vez los datos de demostración.

## Despliegue en Vercel (frontend)

**La página carga pero no aparecen datos; las peticiones van a `vercel.app/api/...`**
Falta `VITE_API_URL` o se definió después de compilar. Vite la incrusta al compilar: definirla en **Settings → Environment Variables** y volver a desplegar (**Deployments → Redeploy**).

**Las peticiones fallan con una doble barra o con `/api/api/...`**
`VITE_API_URL` debe ser solo el dominio de Render, sin barra final y sin `/api`, por ejemplo `https://sga-api-ip10.onrender.com`.

**Error de CORS en la consola (`blocked by CORS policy`)**
El dominio de Vercel no coincide con `SGA_CORS_ORIGENES` en Render. Debe ser exacto: con `https://`, sin barra final, y el dominio de producción (no el de una vista previa de Vercel). Varios dominios se separan con coma.

**Al recargar una página interna aparece 404**
Falta la redirección a `index.html`. Comprobar que el proyecto de Vercel usa `sga/frontend` como **Root Directory**, donde está `vercel.json`.

**Vercel no encuentra `package.json` o compila otra cosa**
El **Root Directory** debe ser `sga/frontend`.

**Las imágenes subidas no se ven**
Con Cloudinary las URLs son absolutas (`https://res.cloudinary.com/...`). Si una imagen es relativa (`/archivos/...`), se subió con el almacén local, que en Render no persiste. Volver a subirla con `SGA_IMAGENES=cloudinary`.

## Errores de la API (pestaña Network)

| HTTP | Código | Causa frecuente | Solución |
|---|---|---|---|
| 401 | `NO_AUTENTICADO` | El token venció (60 minutos) o se cambió `SGA_JWT_SECRETO` | Cerrar sesión e ingresar de nuevo |
| 401 | `CREDENCIALES_INVALIDAS` | Correo o contraseña incorrectos. En el despliegue, la contraseña de demostración es la de `SGA_CONTRASENA_DEMO` | Usar la contraseña configurada en Render |
| 403 | `ACCESO_DENEGADO` | El rol no tiene permiso (por ejemplo, el personal de servicio consultando una reserva) o el recurso es de otro huésped | Ingresar con un rol autorizado |
| 409 | `NOCHES_NO_DISPONIBLES` y similares | Doble venta, transición de estado inválida, correo repetido | Es una regla del negocio: leer el mensaje |
| 422 | Varios | Se viola una regla (capacidad, anticipo, estancia mínima, contraseña insegura) | Corregir los datos según el mensaje |
| 503 | — | Cloudinary no respondió al subir una imagen | Reintentar o pegar la URL de una imagen publicada |

## Correo (Brevo)

**No llega el correo y el registro dice `Couldn't connect to host` o hay un tiempo de espera**
Render gratuito bloquea los puertos SMTP 25, 465 y 587. Usar el puerto **2525** (`SGA_SMTP_PUERTO=2525`), que Brevo acepta.

**`535 Authentication failed`**
`SGA_SMTP_USUARIO` debe ser el *login* SMTP de Brevo (`…@smtp-brevo.com`) y `SGA_SMTP_CLAVE` una **clave SMTP**, no la clave de API de Brevo. Generar una nueva en **Configuración → SMTP y API → SMTP** y copiarla completa: Brevo solo la muestra una vez.

**Brevo rechaza el remitente**
`SGA_SMTP_REMITENTE` debe ser exactamente un remitente verificado en **Configuración → Remitentes**.

**El correo llega a spam**
Ocurre con remitentes de Gmail enviados por un tercero sin dominio propio. Marcarlo como "No es spam". Para evitarlo del todo hace falta un dominio propio autenticado en Brevo.

**La reserva se creó aunque el correo falló**
Es lo esperado: las notificaciones se envían en segundo plano y nunca bloquean la operación. El error queda en el registro con `No se pudo notificar`.

## IA (OpenRouter)

**El botón *Sugerir* muestra "La sugerencia de IA no está disponible en este momento"**
El modelo no respondió a tiempo o devolvió un error. El registro dice `El modelo de IA no respondió correctamente:` seguido de la causa:

| Causa en el registro | Solución |
|---|---|
| `401` | `SGA_IA_CLAVE` incorrecta o revocada |
| `404 No endpoints found matching your data policy` | En OpenRouter, **Settings → Privacy**, permitir los modelos gratuitos |
| `404` o modelo no encontrado | El modelo gratuito fue retirado. Elegir otro con sufijo `:free` en openrouter.ai/models y cambiar `SGA_IA_MODELO` |
| `429` | Se alcanzó el límite diario o por minuto de los modelos gratuitos. Esperar o cambiar de modelo |
| Tiempo de espera | El modelo tardó más de `SGA_IA_TIMEOUT_MS`. Subir el valor o elegir un modelo más rápido, sin razonamiento |

En todos los casos la novedad se registra igual: la persona elige la gravedad.

**El botón *Sugerir* está deshabilitado**
`SGA_IA_HABILITADA=false`. Es la forma de demostrar el sistema sin IA.

## Imágenes (Cloudinary)

**`503` al subir una imagen o el registro menciona `CLOUDINARY_URL`**
Falta la variable o tiene un formato incorrecto. Debe ser `cloudinary://<clave>:<secreto>@<nube>`, copiada del panel de Cloudinary. Si se definen `SGA_CLOUDINARY_NUBE`, `SGA_CLOUDINARY_CLAVE` y `SGA_CLOUDINARY_SECRETO`, tienen prioridad sobre `CLOUDINARY_URL`.

**`Invalid Signature`**
El secreto de la API no corresponde a la clave. Copiar de nuevo el valor completo de `CLOUDINARY_URL`.

**La imagen se rechaza**
Solo se aceptan JPG, PNG o WEBP de hasta 5 MB.

## Git

**`git status` muestra muchos archivos sin seguimiento (`??`)**
Agregar solo los archivos del cambio, con su ruta (`git add -- ruta/archivo`). Nunca `git add .`: puede subir archivos locales o secretos.

**Subí una clave por error**
Borrarla del archivo no basta, porque queda en el historial. Revocarla y generar una nueva en el servicio correspondiente (Render, Cloudinary, Brevo u OpenRouter), y actualizar la variable de entorno.

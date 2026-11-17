# SGA — Frontend

Interfaz web de Aldea Bahareque: React 19 + TypeScript + Vite, con Material UI y la paleta "Bahareque" (cafetal, teja, guadua y cal).

Publicada en **https://sga-aldea-bahareque.vercel.app**, conectada a la API de Render.

## Requisitos

- Node.js 20 o superior
- El backend corriendo en `http://localhost:9090` (desde la carpeta `sga`: `gradlew bootRun`)

## Ejecución

```bash
npm install
npm run dev          # http://localhost:5173 — /api se envía al backend
npm run build        # compila y verifica tipos
npm run lint         # oxlint
```

## Variables de entorno

Se copian de `.env.example` a `.env.local` (no se versiona). En Vercel se definen en **Settings → Environment Variables**.

| Variable | Uso | En desarrollo | En Vercel |
|---|---|---|---|
| `VITE_API_URL` | Dominio del backend, sin barra final y sin `/api` | Vacía: Vite reenvía `/api` al puerto 9090 | `https://sga-api-ip10.onrender.com` |
| `VITE_MAPBOX_TOKEN` | Mapa de ubicación de Mapbox (opcional) | Opcional | Opcional |

Sin token de Mapbox, la ubicación se muestra con un enlace a OpenStreetMap y todo funciona igual.

Vite incrusta estas variables **al compilar**: si se cambian en Vercel, hay que volver a desplegar el frontend para que tomen efecto.

## Despliegue en Vercel

- **Root Directory:** `sga/frontend`.
- `vercel.json` define la compilación (`npm ci`, `npm run build`, carpeta `dist`) y redirige todas las rutas a `index.html`, para que las rutas de React funcionen al recargar la página.
- Cada push a `main` vuelve a desplegar.
- El dominio de Vercel debe estar en `SGA_CORS_ORIGENES` del backend en Render; si no, el navegador bloquea las peticiones (CORS).

Si la API de Render estaba suspendida, la primera petición tarda unos 2 minutos mientras arranca; el frontend carga de inmediato porque Vercel lo sirve como archivos estáticos. Ver [../docs/despliegue.md](../docs/despliegue.md).

## Cómo habla con la API

- `src/api/cliente.ts` arma cada petición como `${VITE_API_URL}/api/...`, agrega el token (`Authorization: Bearer`) y convierte los errores de la API (`codigo` y `mensaje`) en mensajes para la persona.
- `urlRecurso()` completa las URLs relativas de archivos (`/archivos/...`) con el dominio de la API. Las imágenes de Cloudinary ya son absolutas.
- La sesión (token, nombre y rol) se guarda en el navegador. El token vence a los 60 minutos; entonces la aplicación pide iniciar sesión de nuevo.

## Estructura

```
src/
├── main.tsx, rutas.tsx  # Arranque y rutas de la aplicación por rol
├── api/            # Cliente HTTP (token, errores de negocio), tipos y servicios de la API
├── sesion/         # Sesión JWT y rutas protegidas por rol
├── tema/           # Tema de Material UI con la paleta Bahareque
├── componentes/    # Layout, avisos y componentes reutilizables del negocio (incluye novedades con IA)
├── utilidades/     # Formatos (pesos, fechas) y estilos de estados
└── paginas/
    ├── publico/    # Búsqueda, detalle del apartamento, ingreso, registro, recuperación de contraseña, política
    ├── huesped/    # Mis reservas y perfil
    ├── reserva/    # Detalle de la reserva y su folio (compartido por roles)
    ├── recepcion/  # Tablero del día, listado, reserva directa y ocupación
    ├── servicio/   # Preparación de apartamentos y novedades
    └── admin/      # Alojamiento, apartamentos e imágenes, tarifas y políticas, usuarios, canales, reportes y recordatorios
```

Las reglas de negocio viven en el backend. El frontend valida el formato de los datos para orientar al usuario, pero cualquier regla la decide el dominio y su mensaje se muestra tal cual.

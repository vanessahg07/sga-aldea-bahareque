# SGA — Frontend

Interfaz web de Aldea Bahareque: React 19 + TypeScript + Vite, con Material UI y la paleta "Bahareque" (cafetal, teja, guadua y cal).

## Requisitos

- Node.js LTS
- El backend corriendo en `http://localhost:9090` (desde la raíz del repositorio: `gradlew bootRun`)

## Ejecución

```bash
npm install
npm run dev          # http://localhost:5173 — /api se envía al backend
npm run build        # compila y verifica tipos
npm run lint         # oxlint
```

Opcional: copie `.env.example` como `.env.local` y defina `VITE_MAPBOX_TOKEN` para ver el mapa de Mapbox. Sin token, la ubicación se muestra con un enlace a OpenStreetMap y todo funciona igual.

## Estructura

```
src/
├── api/            # Cliente HTTP (token, errores de negocio), tipos y servicios de la API
├── sesion/         # Sesión JWT y rutas protegidas por rol
├── tema/           # Tema de Material UI con la paleta Bahareque
├── componentes/    # Layout, avisos y componentes reutilizables del negocio
├── utilidades/     # Formatos (pesos, fechas) y estilos de estados
└── paginas/
    ├── publico/    # Búsqueda, detalle del apartamento, ingreso, registro, política
    ├── huesped/    # Mis reservas y perfil
    ├── reserva/    # Detalle de la reserva y su folio (compartido por roles)
    ├── recepcion/  # Tablero del día, listado, reserva directa y ocupación
    ├── servicio/   # Preparación de apartamentos y novedades
    └── admin/      # Inventario, tarifas y políticas, alojamiento y usuarios
```

Las reglas de negocio viven en el backend. El frontend valida el formato de los datos para orientar al usuario, pero cualquier regla la decide el dominio y su mensaje se muestra tal cual.

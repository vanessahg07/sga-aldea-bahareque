/// <reference types="vite/client" />

/** Variables de entorno del frontend (se definen en .env.local o en el panel de Vercel). */
interface ImportMetaEnv {
  /** Dominio del backend desplegado, por ejemplo https://sga-api.onrender.com. Vacío en desarrollo. */
  readonly VITE_API_URL?: string
  /** Token público de Mapbox para el mapa de ubicación (opcional). */
  readonly VITE_MAPBOX_TOKEN?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

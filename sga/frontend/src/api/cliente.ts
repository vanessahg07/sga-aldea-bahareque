/** Cliente HTTP de la API: agrega el token de sesión y traduce los errores de negocio. */

export class ErrorApi extends Error {
  readonly estado: number
  readonly codigo: string
  readonly detalles: string[]

  constructor(estado: number, codigo: string, mensaje: string, detalles: string[] = []) {
    super(mensaje)
    this.estado = estado
    this.codigo = codigo
    this.detalles = detalles
  }
}

let tokenActual: string | null = null
let alExpirarSesion: (() => void) | null = null

export function configurarSesion(token: string | null, expiracion: (() => void) | null) {
  tokenActual = token
  alExpirarSesion = expiracion
}

type Metodo = 'GET' | 'POST' | 'PUT' | 'DELETE'

async function solicitar<T>(metodo: Metodo, ruta: string, cuerpo?: unknown): Promise<T> {
  const cabeceras: Record<string, string> = { Accept: 'application/json' }
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json'
  if (tokenActual) cabeceras.Authorization = `Bearer ${tokenActual}`

  let respuesta: Response
  try {
    respuesta = await fetch(`/api${ruta}`, {
      method: metodo,
      headers: cabeceras,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    })
  } catch {
    throw new ErrorApi(0, 'SIN_CONEXION', 'No fue posible comunicarse con el servidor. Verifique su conexión.')
  }

  if (respuesta.status === 401 && tokenActual && alExpirarSesion) {
    alExpirarSesion()
  }
  if (respuesta.status === 204) {
    return undefined as T
  }
  const texto = await respuesta.text()
  const datos = texto ? JSON.parse(texto) : undefined
  if (!respuesta.ok) {
    throw new ErrorApi(respuesta.status, datos?.codigo ?? 'ERROR', datos?.mensaje ?? 'Ocurrió un error inesperado',
      datos?.detalles ?? [])
  }
  return datos as T
}

/** Sube un formulario con archivos (multipart); el navegador arma el encabezado Content-Type. */
async function enviarFormulario<T>(ruta: string, formulario: FormData): Promise<T> {
  let respuesta: Response
  try {
    respuesta = await fetch(`/api${ruta}`, {
      method: 'POST',
      headers: tokenActual ? { Authorization: `Bearer ${tokenActual}` } : {},
      body: formulario,
    })
  } catch {
    throw new ErrorApi(0, 'SIN_CONEXION', 'No fue posible comunicarse con el servidor. Verifique su conexión.')
  }
  const texto = await respuesta.text()
  const datos = texto ? JSON.parse(texto) : undefined
  if (!respuesta.ok) {
    throw new ErrorApi(respuesta.status, datos?.codigo ?? 'ERROR', datos?.mensaje ?? 'Ocurrió un error inesperado',
      datos?.detalles ?? [])
  }
  return datos as T
}

/** Descarga un archivo de la API con la sesión actual y lo guarda con el nombre que indique el servidor. */
async function descargar(ruta: string, nombrePorDefecto: string): Promise<void> {
  const respuesta = await fetch(`/api${ruta}`, { headers: tokenActual ? { Authorization: `Bearer ${tokenActual}` } : {} })
  if (!respuesta.ok) {
    const datos = await respuesta.json().catch(() => undefined)
    throw new ErrorApi(respuesta.status, datos?.codigo ?? 'ERROR', datos?.mensaje ?? 'No se pudo descargar el archivo')
  }
  const nombre = /filename="([^"]+)"/.exec(respuesta.headers.get('Content-Disposition') ?? '')?.[1] ?? nombrePorDefecto
  const enlace = document.createElement('a')
  enlace.href = URL.createObjectURL(await respuesta.blob())
  enlace.download = nombre
  enlace.click()
  URL.revokeObjectURL(enlace.href)
}

export const api = {
  get: <T>(ruta: string) => solicitar<T>('GET', ruta),
  post: <T>(ruta: string, cuerpo?: unknown) => solicitar<T>('POST', ruta, cuerpo ?? {}),
  put: <T>(ruta: string, cuerpo?: unknown) => solicitar<T>('PUT', ruta, cuerpo ?? {}),
  delete: (ruta: string) => solicitar<void>('DELETE', ruta),
  formulario: enviarFormulario,
  descargar,
}

/** Arma un query string ignorando los valores vacíos. */
export function consulta(parametros: Record<string, string | number | boolean | null | undefined>): string {
  const busqueda = new URLSearchParams()
  Object.entries(parametros).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== '') busqueda.set(clave, String(valor))
  })
  const texto = busqueda.toString()
  return texto ? `?${texto}` : ''
}

/** Mensaje legible de cualquier error, incluidos los de validación por campo. */
export function mensajeDe(error: unknown): string {
  if (error instanceof ErrorApi) {
    return error.detalles.length ? `${error.message}: ${error.detalles.join('; ')}` : error.message
  }
  return error instanceof Error ? error.message : 'Ocurrió un error inesperado'
}

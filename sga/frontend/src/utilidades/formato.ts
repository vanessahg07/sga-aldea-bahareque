/** Formatos de presentación en español de Colombia. */

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
const fechaCorta = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' })
const fechaLarga = new Intl.DateTimeFormat('es-CO', { weekday: 'short', day: 'numeric', month: 'short' })
const fechaHoraCorta = new Intl.DateTimeFormat('es-CO', {
  day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
})

export const formatoPesos = (valor: number | null | undefined) => (valor == null ? '—' : pesos.format(valor))

/** Interpreta 'AAAA-MM-DD' como fecha local (sin desfase por zona horaria). */
export function aFecha(iso: string): Date {
  const [a, m, d] = iso.slice(0, 10).split('-').map(Number)
  return new Date(a, m - 1, d)
}

export const formatoFecha = (iso: string | null | undefined) => (iso ? fechaCorta.format(aFecha(iso)) : '—')
export const formatoDia = (iso: string) => fechaLarga.format(aFecha(iso))
export const formatoFechaHora = (iso: string | null | undefined) => (iso ? fechaHoraCorta.format(new Date(iso)) : '—')
export const formatoHora = (hora: string | null | undefined) => (hora ? hora.slice(0, 5) : '—')

/** Fecha local en formato 'AAAA-MM-DD'. */
export function isoLocal(fecha: Date): string {
  const m = String(fecha.getMonth() + 1).padStart(2, '0')
  const d = String(fecha.getDate()).padStart(2, '0')
  return `${fecha.getFullYear()}-${m}-${d}`
}

export function hoyIso(): string {
  return isoLocal(new Date())
}

export function sumarDias(iso: string, dias: number): string {
  const fecha = aFecha(iso)
  fecha.setDate(fecha.getDate() + dias)
  return isoLocal(fecha)
}

export function noches(entrada: string, salida: string): number {
  return Math.round((aFecha(salida).getTime() - aFecha(entrada).getTime()) / 86_400_000)
}

/** Fecha de nacimiento aproximada para una edad dada a una fecha de referencia. */
export function nacimientoParaEdad(edad: number, referencia: string): string {
  const fecha = aFecha(referencia)
  fecha.setFullYear(fecha.getFullYear() - edad)
  fecha.setMonth(fecha.getMonth() - 6)
  return isoLocal(fecha)
}

/** Grupo de búsqueda: adultos y menores del umbral facturable, convertidos a fechas de nacimiento. */
export function fechasDelGrupo(adultos: number, menores: number, entrada: string): string[] {
  return [
    ...Array.from({ length: adultos }, () => nacimientoParaEdad(30, entrada)),
    ...Array.from({ length: menores }, () => nacimientoParaEdad(2, entrada)),
  ]
}

/** 'vista-panoramica' → 'Vista panorámica' (etiquetas legibles para las características). */
export function etiqueta(texto: string): string {
  const conEspacios = texto.replace(/[-_]/g, ' ').toLowerCase().replace(/anio/g, 'año').replace(/banos/g, 'baños')
  return conEspacios.charAt(0).toUpperCase() + conEspacios.slice(1)
}

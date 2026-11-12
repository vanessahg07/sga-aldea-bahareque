import { colores } from '../tema/tema'
import type { Canal, EstadoOperativo, EstadoReserva, Gravedad, Rol, TipoCanal, TipoCargo, TipoEventoCanal } from '../api/tipos'

interface Estilo {
  texto: string
  fondo: string
  color: string
}

export const estiloReserva: Record<EstadoReserva, Estilo> = {
  PENDIENTE: { texto: 'Pendiente', fondo: colores.guaduaClara, color: colores.guaduaOscura },
  CONFIRMADA: { texto: 'Confirmada', fondo: colores.cafetalClaro, color: colores.cafetalOscuro },
  EN_CURSO: { texto: 'En curso', fondo: colores.niebla, color: colores.nieblaOscura },
  FINALIZADA: { texto: 'Finalizada', fondo: '#ECE7DE', color: '#4F463C' },
  CANCELADA: { texto: 'Cancelada', fondo: colores.tejaClara, color: colores.tejaOscura },
  NO_SHOW: { texto: 'No-show', fondo: '#EADFD3', color: '#5B3A22' },
}

export const estiloOperativo: Record<EstadoOperativo, Estilo> = {
  PREPARADO: { texto: 'Preparado', fondo: colores.cafetalClaro, color: colores.cafetalOscuro },
  OCUPADO: { texto: 'Ocupado', fondo: colores.niebla, color: colores.nieblaOscura },
  PENDIENTE_PREPARACION: { texto: 'Pendiente de preparación', fondo: colores.guaduaClara, color: colores.guaduaOscura },
  EN_PREPARACION: { texto: 'En preparación', fondo: '#EFE3C4', color: '#5E4A12' },
  FUERA_DE_SERVICIO: { texto: 'Fuera de servicio', fondo: colores.tejaClara, color: colores.tejaOscura },
}

export const estiloGravedad: Record<Gravedad, Estilo> = {
  BAJA: { texto: 'Baja', fondo: colores.cafetalClaro, color: colores.cafetalOscuro },
  MEDIA: { texto: 'Media', fondo: colores.guaduaClara, color: colores.guaduaOscura },
  ALTA: { texto: 'Alta', fondo: colores.tejaClara, color: colores.tejaOscura },
  CRITICA: { texto: 'Crítica', fondo: colores.teja, color: colores.papel },
}

export const nombreCanal: Record<Canal, string> = { PORTAL: 'Portal', DIRECTO: 'Directo', EXTERNO: 'Externo' }

export const nombreRol: Record<Rol, string> = {
  HUESPED: 'Huésped',
  RECEPCIONISTA: 'Recepción',
  ADMINISTRADOR: 'Administración',
  PERSONAL_SERVICIO: 'Personal de servicio',
}

export const nombreCargo: Record<TipoCargo, string> = {
  ALOJAMIENTO: 'Alojamiento',
  MASCOTA: 'Mascota',
  SERVICIO_ADICIONAL: 'Servicio',
  AJUSTE: 'Ajuste',
  PENALIDAD: 'Penalidad',
}

export const ESTADOS_RESERVA = Object.keys(estiloReserva) as EstadoReserva[]
export const ESTADOS_OPERATIVOS = Object.keys(estiloOperativo) as EstadoOperativo[]
export const GRAVEDADES = Object.keys(estiloGravedad) as Gravedad[]
export const ROLES = Object.keys(nombreRol) as Rol[]
export const CANALES = Object.keys(nombreCanal) as Canal[]

export const nombreTipoCanal: Record<TipoCanal, string> = {
  AGENCIA_EN_LINEA: 'Agencia en línea',
  AGENCIA_DE_VIAJES: 'Agencia de viajes',
  METABUSCADOR: 'Metabuscador',
}
export const TIPOS_CANAL = Object.keys(nombreTipoCanal) as TipoCanal[]

export const estiloEventoCanal: Record<TipoEventoCanal, Estilo> = {
  CONSULTA_DISPONIBILIDAD: { texto: 'Consulta', fondo: colores.niebla, color: colores.nieblaOscura },
  RESERVA_ACEPTADA: { texto: 'Reserva aceptada', fondo: colores.cafetalClaro, color: colores.cafetalOscuro },
  RESERVA_DUPLICADA: { texto: 'Mensaje repetido', fondo: colores.guaduaClara, color: colores.guaduaOscura },
  RESERVA_EN_CONFLICTO: { texto: 'Conflicto', fondo: colores.tejaClara, color: colores.tejaOscura },
  RESERVA_RECHAZADA: { texto: 'Reserva rechazada', fondo: '#EADFD3', color: '#5B3A22' },
  CANCELACION_ACEPTADA: { texto: 'Cancelación', fondo: '#ECE7DE', color: '#4F463C' },
  CANCELACION_REPETIDA: { texto: 'Cancelación repetida', fondo: colores.guaduaClara, color: colores.guaduaOscura },
  CANCELACION_RECHAZADA: { texto: 'Cancelación rechazada', fondo: '#EADFD3', color: '#5B3A22' },
  CREDENCIAL_INVALIDA: { texto: 'Credencial inválida', fondo: colores.tejaClara, color: colores.tejaOscura },
}

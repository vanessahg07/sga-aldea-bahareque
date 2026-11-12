/** Tipos de la API del SGA. Reflejan las respuestas de los controladores REST. */

export type Rol = 'HUESPED' | 'RECEPCIONISTA' | 'ADMINISTRADOR' | 'PERSONAL_SERVICIO'
export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA' | 'NO_SHOW'
export type Canal = 'PORTAL' | 'DIRECTO' | 'EXTERNO'
export type EstadoOperativo = 'PREPARADO' | 'OCUPADO' | 'PENDIENTE_PREPARACION' | 'EN_PREPARACION' | 'FUERA_DE_SERVICIO'
export type EstadoNoche = 'DISPONIBLE' | 'RESERVADA' | 'BLOQUEADA' | 'NO_A_LA_VENTA'
export type Gravedad = 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA'
export type TipoCargo = 'ALOJAMIENTO' | 'MASCOTA' | 'SERVICIO_ADICIONAL' | 'AJUSTE' | 'PENALIDAD'

export interface Pagina<T> {
  contenido: T[]
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

export interface Usuario {
  id: string
  nombre: string
  correo: string
  telefono: string
  documento: string
  fechaNacimiento: string
  fotoUrl: string | null
  rol: Rol
}

export interface Sesion {
  token: string
  tipo: string
  expiraEn: string
  usuario: Usuario
}

export interface ServicioAdicional {
  codigo: string
  nombre: string
  descripcion: string | null
  generaCargo: boolean
  valor: number
  unidad: string | null
}

export interface Parametros {
  umbralEdadFacturable: number
  horaEntrada: string
  horaSalida: string
  horasPreparacion: number
  horasPlazoConfirmacion: number
  horaLimiteNoShow: string
  porcentajeAnticipo: number
  maxMascotas: number
  cargoMascota: number
  nochesDescuentoLargo: number
  porcentajeDescuentoLargo: number
  diasRecordatorioLlegada: number
}

export interface Alojamiento {
  nombre: string
  descripcion: string | null
  ciudad: string
  direccion: string | null
  latitud: number
  longitud: number
  normas: string[]
  servicios: ServicioAdicional[]
  mediosPago: string[]
  parametros: Parametros
}

export interface Imagen {
  url: string
  principal: boolean
}

export interface Apartamento {
  codigo: string
  nombre: string
  descripcion: string | null
  dormitorios: number
  capacidad: number
  caracteristicas: string[]
  admiteMascotas: boolean
  imagenes: Imagen[]
  imagenPrincipal: string | null
  activo: boolean
  estadoOperativo: EstadoOperativo
}

export interface ActualizacionApartamento {
  apartamento: Apartamento
  reservasSobreCapacidad: ReservaResumen[]
  advertencia: string | null
}

export interface Tarjeta {
  codigo: string
  nombre: string
  imagenPrincipal: string | null
  capacidad: number
  dormitorios: number
  caracteristicas: string[]
  noches: number
  valorEstimado: number
  latitud: number
  longitud: number
}

export interface Noche {
  fecha: string
  estado: EstadoNoche
}

export interface Bloqueo {
  id: string
  codigoApartamento: string
  desde: string
  hasta: string
  motivo: string
}

export interface Novedad {
  id: string
  codigoApartamento: string
  fecha: string
  autor: string
  descripcion: string
  gravedad: Gravedad
}

export interface Rango {
  inicio: string
  fin: string
}

export interface Temporada {
  nombre: string
  esBase: boolean
  rangos: Rango[]
  estanciaMinimaNoches: number
}

export interface Tarifa {
  codigoApartamento: string
  temporada: string
  valor: number
  vigenteDesde: string
}

export interface Tramo {
  diasAntelacionMinimos: number
  porcentajeRetencion: number
}

export interface Politica {
  version: number
  vigenteDesde: string
  tramos: Tramo[]
  porcentajeRetencionNoShow: number
}

export interface DetalleNoche {
  fecha: string
  temporada: string
  tarifa: number
  ocupantesFacturables: number
  subtotal: number
}

export interface Cotizacion {
  detalle: DetalleNoche[]
  subtotalNoches: number
  descuento: number
  valorAlojamiento: number
  valorMascotas: number
  valorTotal: number
}

export interface ResultadoCotizacion {
  codigoApartamento: string
  entrada: string
  salida: string
  noches: number
  disponible: boolean
  motivoNoDisponible: string | null
  mensajeNoDisponible: string | null
  cotizacion: Cotizacion
}

export interface Ocupante {
  nombre: string
  fechaNacimiento: string
}

export interface Titular {
  nombre: string
  documento: string
  correo: string | null
  telefono: string | null
  fechaNacimiento: string
  tieneCuenta: boolean
}

export interface Reserva {
  id: string
  estado: EstadoReserva
  canal: Canal
  referenciaExterna: string | null
  codigoApartamento: string
  entrada: string
  salida: string
  noches: number
  horaEstimadaLlegada: string | null
  titular: Titular
  acompanantes: Ocupante[]
  totalOcupantes: number
  mascotas: number
  creadaEn: string
  versionPolitica: number
  cotizacion: Cotizacion
  saldo: number
  folioCerrado: boolean
}

export interface ReservaResumen {
  id: string
  estado: EstadoReserva
  canal: Canal
  codigoApartamento: string
  entrada: string
  salida: string
  titular: string
  totalOcupantes: number
  horaEstimadaLlegada: string | null
  valorTotal: number
  saldo: number
  creadaEn: string
}

export interface Cargo {
  id: string
  tipo: TipoCargo
  valor: number
  descripcion: string
  fecha: string
  idCargoRevertido: string | null
}

export interface Pago {
  id: string
  valor: number
  medio: string
  fecha: string
  idPagoRevertido: string | null
}

export interface Folio {
  id: string
  idReserva: string
  estadoReserva: EstadoReserva
  cargos: Cargo[]
  pagos: Pago[]
  totalCargos: number
  totalPagos: number
  saldo: number
  cerrado: boolean
  autorizacionCierre: { autor: string; motivo: string; fecha: string } | null
}

// --- Canales externos (Corte 4) ---

export type TipoCanal = 'AGENCIA_EN_LINEA' | 'AGENCIA_DE_VIAJES' | 'METABUSCADOR'
export type TipoEventoCanal = 'CONSULTA_DISPONIBILIDAD' | 'RESERVA_ACEPTADA' | 'RESERVA_DUPLICADA' | 'RESERVA_EN_CONFLICTO'
  | 'RESERVA_RECHAZADA' | 'CANCELACION_ACEPTADA' | 'CANCELACION_REPETIDA' | 'CANCELACION_RECHAZADA' | 'CREDENCIAL_INVALIDA'

export interface CanalVenta {
  codigo: string
  nombre: string
  tipo: TipoCanal
  activo: boolean
}

export interface CredencialCanal {
  canal: CanalVenta
  credencial: string
  aviso: string
}

export interface ConflictoCanal {
  id: string
  canal: string
  idExterno: string
  codigoApartamento: string
  entrada: string
  salida: string
  titular: string
  motivo: string
  mensaje: string
  registradoEn: string
  revisado: boolean
  revisadoPor: string | null
  notaRevision: string | null
  fechaRevision: string | null
}

export interface EventoCanal {
  id: string
  canal: string
  tipo: TipoEventoCanal
  idExterno: string | null
  idReserva: string | null
  detalle: string
  fecha: string
}

// --- Reportes ---

export interface ReporteOcupacion {
  desde: string
  hasta: string
  apartamentosActivos: number
  nochesDisponibles: number
  nochesVendidas: number
  porcentaje: number
  porApartamento: { codigoApartamento: string; nochesVendidas: number; porcentaje: number }[]
}

export interface ReporteIngresos {
  desde: string
  hasta: string
  porMedio: { medio: string; total: number; pagos: number }[]
  total: number
  cantidadPagos: number
}

export interface ReporteCanales {
  desde: string
  hasta: string
  canales: { canal: Canal; porEstado: Record<EstadoReserva, number>; total: number }[]
  total: number
}

export interface Indicadores {
  desde: string
  hasta: string
  ingresoAlojamiento: number
  tarifaPromedioNoche: number
  ingresoPorNocheDisponible: number
  tasaNoShow: number
  tasaCancelacion: number
  antelacionPromedioDias: number
}

// --- Recordatorios, IA e imágenes ---

export interface Recordatorio {
  id: string
  idReserva: string
  tipo: 'LLEGADA' | 'SALIDA'
  destinatario: string
  asunto: string
  enviadoEn: string
}

export interface EstadoIa {
  habilitada: boolean
  proveedor: string
}

export interface SugerenciaGravedad {
  disponible: boolean
  gravedad: Gravedad | null
  justificacion: string | null
  proveedor: string | null
  mensaje: string | null
}

import { api, consulta } from './cliente'
import type {
  ActualizacionApartamento, Alojamiento, Apartamento, Bloqueo, Canal, CanalVenta, ConflictoCanal, CredencialCanal,
  EstadoIa, EstadoOperativo, EstadoReserva, EventoCanal, Folio, Gravedad, Imagen, Indicadores, Noche, Novedad,
  Ocupante, Pagina, Parametros, Politica, Recordatorio, ReporteCanales, ReporteIngresos, ReporteOcupacion, Reserva,
  ReservaResumen, ResultadoCotizacion, Rol, ServicioAdicional, Sesion, SugerenciaGravedad, Tarifa, Tarjeta,
  Temporada, TipoCanal, Tramo, Usuario,
} from './tipos'

// --- Autenticación y perfil ---

export interface DatosRegistro {
  nombre: string
  correo: string
  contrasena: string
  telefono: string
  documento: string
  fechaNacimiento: string
}

export const autenticacion = {
  ingresar: (correo: string, contrasena: string) => api.post<Sesion>('/auth/login', { correo, contrasena }),
  registrar: (datos: DatosRegistro) => api.post<Sesion>('/auth/registro', datos),
  solicitarCodigo: (correo: string) => api.post<{ mensaje: string }>('/auth/recuperacion', { correo }),
  restablecer: (correo: string, codigo: string, nuevaContrasena: string) =>
    api.post<{ mensaje: string }>('/auth/recuperacion/restablecimiento', { correo, codigo, nuevaContrasena }),
}

export const perfil = {
  consultar: () => api.get<Usuario>('/perfil'),
  actualizar: (nombre: string, telefono: string, fotoUrl: string | null) =>
    api.put<Usuario>('/perfil', { nombre, telefono, fotoUrl }),
  cambiarContrasena: (contrasenaActual: string, nuevaContrasena: string) =>
    api.put<{ mensaje: string }>('/perfil/contrasena', { contrasenaActual, nuevaContrasena }),
}

export const usuarios = {
  listar: (pagina: number) => api.get<Pagina<Usuario>>(`/usuarios${consulta({ pagina })}`),
  crear: (datos: DatosRegistro & { rol: Rol }) => api.post<Usuario>('/usuarios', datos),
  asignarRol: (id: string, rol: Rol) => api.put<Usuario>(`/usuarios/${id}/rol`, { rol }),
  eliminar: (id: string) => api.delete(`/usuarios/${id}`),
}

// --- Alojamiento ---

export const alojamiento = {
  consultar: () => api.get<Alojamiento>('/alojamiento'),
  actualizarDatos: (datos: Pick<Alojamiento, 'nombre' | 'descripcion' | 'ciudad' | 'direccion' | 'latitud' | 'longitud' | 'normas'>) =>
    api.put<Alojamiento>('/alojamiento', datos),
  cambiarParametros: (parametros: Parametros) => api.put<Alojamiento>('/alojamiento/parametros', parametros),
  definirServicios: (servicios: ServicioAdicional[]) => api.put<Alojamiento>('/alojamiento/servicios', servicios),
  definirMediosPago: (medios: string[]) => api.put<Alojamiento>('/alojamiento/medios-pago', { medios }),
}

// --- Apartamentos ---

export interface DatosApartamento {
  nombre: string
  descripcion: string
  dormitorios: number
  capacidad: number
  caracteristicas: string[]
  admiteMascotas: boolean
}

export const apartamentos = {
  aLaVenta: (pagina = 0) => api.get<Pagina<Apartamento>>(`/apartamentos${consulta({ pagina })}`),
  gestion: (pagina = 0) => api.get<Pagina<Apartamento>>(`/apartamentos/gestion${consulta({ pagina })}`),
  consultar: (codigo: string) => api.get<Apartamento>(`/apartamentos/${codigo}`),
  calendario: (codigo: string, desde: string, hasta: string) =>
    api.get<Noche[]>(`/apartamentos/${codigo}/calendario${consulta({ desde, hasta })}`),
  tarifas: (codigo: string) => api.get<Tarifa[]>(`/apartamentos/${codigo}/tarifas`),
  crear: (codigo: string, datos: DatosApartamento) => api.post<Apartamento>('/apartamentos', { codigo, ...datos }),
  actualizar: (codigo: string, datos: DatosApartamento) =>
    api.put<ActualizacionApartamento>(`/apartamentos/${codigo}`, datos),
  definirImagenes: (codigo: string, imagenes: Imagen[]) =>
    api.put<Apartamento>(`/apartamentos/${codigo}/imagenes`, imagenes),
  activar: (codigo: string) => api.post<Apartamento>(`/apartamentos/${codigo}/activacion`),
  desactivar: (codigo: string) => api.post<Apartamento>(`/apartamentos/${codigo}/desactivacion`),
  eliminar: (codigo: string) => api.delete(`/apartamentos/${codigo}`),
  bloqueos: (codigo: string) => api.get<Bloqueo[]>(`/apartamentos/${codigo}/bloqueos`),
  registrarBloqueo: (codigo: string, desde: string, hasta: string, motivo: string) =>
    api.post<Bloqueo>(`/apartamentos/${codigo}/bloqueos`, { desde, hasta, motivo }),
  levantarBloqueo: (id: string) => api.delete(`/apartamentos/bloqueos/${id}`),
  pendientesPreparacion: () => api.get<Apartamento[]>('/apartamentos/pendientes-preparacion'),
  cambiarEstado: (codigo: string, estado: EstadoOperativo) =>
    api.put<Apartamento>(`/apartamentos/${codigo}/estado-operativo`, { estado }),
  registrarNovedad: (codigo: string, descripcion: string, gravedad: Gravedad) =>
    api.post<Novedad>(`/apartamentos/${codigo}/novedades`, { descripcion, gravedad }),
  novedades: (codigo: string, pagina = 0) =>
    api.get<Pagina<Novedad>>(`/apartamentos/${codigo}/novedades${consulta({ pagina })}`),
}

// --- Temporadas, tarifas y política ---

export const tarifas = {
  temporadas: () => api.get<Temporada[]>('/temporadas'),
  crearTemporada: (nombre: string, rangos: Temporada['rangos'], estanciaMinimaNoches: number) =>
    api.post<Temporada>('/temporadas', { nombre, rangos, estanciaMinimaNoches }),
  actualizarTemporada: (nombre: string, rangos: Temporada['rangos'], estanciaMinimaNoches: number) =>
    api.put<Temporada>(`/temporadas/${nombre}`, { rangos, estanciaMinimaNoches }),
  eliminarTemporada: (nombre: string) => api.delete(`/temporadas/${nombre}`),
  registrar: (codigoApartamento: string, temporada: string, valor: number) =>
    api.post<Tarifa>('/tarifas', { codigoApartamento, temporada, valor }),
  historico: (codigo: string) => api.get<Tarifa[]>(`/tarifas/historico/${codigo}`),
  incompletas: () => api.get<string[]>('/tarifas/incompletas'),
  politicaVigente: () => api.get<Politica>('/politicas/vigente'),
  versionesPolitica: () => api.get<Politica[]>('/politicas'),
  publicarPolitica: (tramos: Tramo[], porcentajeRetencionNoShow: number) =>
    api.post<Politica>('/politicas', { tramos, porcentajeRetencionNoShow }),
}

// --- Disponibilidad ---

export interface CriteriosBusqueda {
  entrada: string
  salida: string
  fechasNacimiento: string[]
  capacidadMinima?: number | null
  precioMaximo?: number | null
  caracteristicas: string[]
  mascotas: number
}

export const disponibilidad = {
  buscar: (criterios: CriteriosBusqueda, pagina: number) =>
    api.post<Pagina<Tarjeta>>(`/disponibilidad/busqueda${consulta({ pagina })}`, criterios),
  cotizar: (codigoApartamento: string, entrada: string, salida: string, fechasNacimiento: string[], mascotas: number) =>
    api.post<ResultadoCotizacion>('/cotizaciones', { codigoApartamento, entrada, salida, fechasNacimiento, mascotas }),
}

// --- Reservas ---

export interface DatosEstancia {
  codigoApartamento: string
  entrada: string
  salida: string
  acompanantes: Ocupante[]
  horaEstimadaLlegada: string | null
  mascotas: number
}

export interface DatosTitular {
  nombre: string
  documento: string
  correo: string
  telefono: string
  fechaNacimiento: string
}

export interface FiltroReservas {
  desde?: string
  hasta?: string
  estado?: EstadoReserva | ''
  apartamento?: string
  canal?: Canal | ''
  titular?: string
  pagina: number
}

export const reservas = {
  crearPorPortal: (datos: DatosEstancia) => api.post<Reserva>('/reservas/portal', datos),
  crearDirecta: (titular: DatosTitular, datos: DatosEstancia) =>
    api.post<Reserva>('/reservas/directas', { titular: { ...titular, correo: titular.correo || null }, ...datos }),
  listar: (filtro: FiltroReservas) => api.get<Pagina<ReservaResumen>>(`/reservas${consulta({ ...filtro })}`),
  mias: (pagina: number) => api.get<Pagina<ReservaResumen>>(`/reservas/mias${consulta({ pagina })}`),
  consultar: (id: string) => api.get<Reserva>(`/reservas/${id}`),
  modificar: (id: string, datos: DatosEstancia) => api.put<Reserva>(`/reservas/${id}`, datos),
  registrarHoraLlegada: (id: string, horaEstimadaLlegada: string) =>
    api.put<Reserva>(`/reservas/${id}/hora-llegada`, { horaEstimadaLlegada }),
  confirmar: (id: string) => api.post<Reserva>(`/reservas/${id}/confirmacion`),
  cancelar: (id: string) => api.post<Reserva>(`/reservas/${id}/cancelacion`),
  declararNoShow: (id: string) => api.post<Reserva>(`/reservas/${id}/no-show`),
  registrarLlegada: (id: string) => api.post<Reserva>(`/reservas/${id}/llegada`),
  registrarSalida: (id: string, motivoAutorizacion?: string) =>
    api.post<Reserva>(`/reservas/${id}/salida`, { motivoAutorizacion: motivoAutorizacion || null }),
  llegadas: (fecha: string) => api.get<ReservaResumen[]>(`/reservas/llegadas${consulta({ fecha })}`),
  salidas: (fecha: string) => api.get<ReservaResumen[]>(`/reservas/salidas${consulta({ fecha })}`),
  ocupacion: (desde: string, hasta: string) =>
    api.get<ReservaResumen[]>(`/reservas/ocupacion${consulta({ desde, hasta })}`),
}

export const folios = {
  consultar: (idReserva: string) => api.get<Folio>(`/reservas/${idReserva}/folio`),
  registrarPago: (idReserva: string, valor: number, medio: string) =>
    api.post<Folio>(`/reservas/${idReserva}/folio/pagos`, { valor, medio }),
  cargarServicio: (idReserva: string, codigoServicio: string, cantidad: number) =>
    api.post<Folio>(`/reservas/${idReserva}/folio/servicios`, { codigoServicio, cantidad }),
  registrarAjuste: (idReserva: string, valor: number, descripcion: string) =>
    api.post<Folio>(`/reservas/${idReserva}/folio/ajustes`, { valor, descripcion }),
  revertirCargo: (idReserva: string, idCargo: string, motivo: string) =>
    api.post<Folio>(`/reservas/${idReserva}/folio/cargos/${idCargo}/reverso`, { motivo }),
  revertirPago: (idReserva: string, idPago: string) =>
    api.post<Folio>(`/reservas/${idReserva}/folio/pagos/${idPago}/reverso`),
  cerrar: (idReserva: string, motivoAutorizacion?: string) =>
    api.post<Folio>(`/reservas/${idReserva}/folio/cierre`, { motivoAutorizacion: motivoAutorizacion || null }),
}

// --- Canales externos ---

export const canales = {
  listar: () => api.get<CanalVenta[]>('/canales'),
  registrar: (codigo: string, nombre: string, tipo: TipoCanal) =>
    api.post<CredencialCanal>('/canales', { codigo, nombre, tipo }),
  actualizar: (codigo: string, nombre: string, tipo: TipoCanal) =>
    api.put<CanalVenta>(`/canales/${codigo}`, { nombre, tipo }),
  renovarCredencial: (codigo: string) => api.post<CredencialCanal>(`/canales/${codigo}/credencial`),
  cambiarActivo: (codigo: string, activo: boolean) => api.put<CanalVenta>(`/canales/${codigo}/activo`, { activo }),
  conflictos: (pendientes: boolean, pagina: number) =>
    api.get<Pagina<ConflictoCanal>>(`/canales/conflictos${consulta({ pendientes, pagina })}`),
  revisarConflicto: (id: string, nota: string) => api.post<ConflictoCanal>(`/canales/conflictos/${id}/revision`, { nota }),
  bitacora: (canal: string, pagina: number) =>
    api.get<Pagina<EventoCanal>>(`/canales/bitacora${consulta({ canal, pagina })}`),
}

// --- Reportes ---

export type TipoReporte = 'ocupacion' | 'ingresos' | 'canales' | 'indicadores'

export const reportes = {
  ocupacion: (desde: string, hasta: string) => api.get<ReporteOcupacion>(`/reportes/ocupacion${consulta({ desde, hasta })}`),
  ingresos: (desde: string, hasta: string) => api.get<ReporteIngresos>(`/reportes/ingresos${consulta({ desde, hasta })}`),
  canales: (desde: string, hasta: string) => api.get<ReporteCanales>(`/reportes/canales${consulta({ desde, hasta })}`),
  indicadores: (desde: string, hasta: string) => api.get<Indicadores>(`/reportes/indicadores${consulta({ desde, hasta })}`),
  descargarCsv: (tipo: TipoReporte, desde: string, hasta: string) =>
    api.descargar(`/reportes/${tipo}/csv${consulta({ desde, hasta })}`, `reporte-${tipo}.csv`),
}

// --- Recordatorios, IA e imágenes ---

export const recordatorios = {
  listar: (pagina: number) => api.get<Pagina<Recordatorio>>(`/recordatorios${consulta({ pagina })}`),
  enviarAhora: () => api.post<Recordatorio[]>('/recordatorios/envios'),
}

export const ia = {
  estado: () => api.get<EstadoIa>('/novedades/ia'),
  sugerirGravedad: (descripcion: string) =>
    api.post<SugerenciaGravedad>('/novedades/sugerencia-gravedad', { descripcion }),
}

export const imagenes = {
  subir: (archivo: File) => {
    const formulario = new FormData()
    formulario.append('archivo', archivo)
    return api.formulario<{ url: string }>('/imagenes', formulario)
  },
}

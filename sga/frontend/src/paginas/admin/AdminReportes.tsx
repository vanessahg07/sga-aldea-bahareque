import { useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box, Button, Card, CardContent, Container, Grid, LinearProgress, Stack, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography,
} from '@mui/material'
import DescargarIcon from '@mui/icons-material/FileDownloadOutlined'
import { reportes, type TipoReporte } from '../../api/servicios'
import type { EstadoReserva } from '../../api/tipos'
import { useAvisos } from '../../componentes/Avisos'
import { CampoFecha, Cargando, Encabezado, MensajeError } from '../../componentes/Comunes'
import { ESTADOS_RESERVA, estiloReserva, nombreCanal } from '../../utilidades/estados'
import { formatoFecha, formatoPesos, hoyIso, isoLocal } from '../../utilidades/formato'
import { colores } from '../../tema/tema'

function mesDe(desplazamiento: number): [string, string] {
  const hoy = new Date()
  return [isoLocal(new Date(hoy.getFullYear(), hoy.getMonth() + desplazamiento, 1)),
    isoLocal(new Date(hoy.getFullYear(), hoy.getMonth() + desplazamiento + 1, 0))]
}

const PERIODOS: { texto: string; rango: () => [string, string] }[] = [
  { texto: 'Este mes', rango: () => mesDe(0) },
  { texto: 'Mes anterior', rango: () => mesDe(-1) },
  { texto: 'Próximo mes', rango: () => mesDe(1) },
  { texto: 'Últimos 90 días', rango: () => { const h = new Date(); h.setDate(h.getDate() - 89); return [isoLocal(h), hoyIso()] } },
]

const porcentaje = (valor: number) => `${valor.toLocaleString('es-CO', { maximumFractionDigits: 2 })} %`

/** Reportes obligatorios (ocupación, ingresos, reservas por canal) e indicadores propios del alojamiento. */
export function AdminReportes() {
  const [inicial, final] = mesDe(0)
  const [desde, setDesde] = useState(inicial)
  const [hasta, setHasta] = useState(final)
  const valido = !!desde && !!hasta && desde <= hasta

  const ocupacion = useQuery({ queryKey: ['reporte', 'ocupacion', desde, hasta], queryFn: () => reportes.ocupacion(desde, hasta), enabled: valido })
  const ingresos = useQuery({ queryKey: ['reporte', 'ingresos', desde, hasta], queryFn: () => reportes.ingresos(desde, hasta), enabled: valido })
  const canales = useQuery({ queryKey: ['reporte', 'canales', desde, hasta], queryFn: () => reportes.canales(desde, hasta), enabled: valido })
  const indicadores = useQuery({ queryKey: ['reporte', 'indicadores', desde, hasta], queryFn: () => reportes.indicadores(desde, hasta), enabled: valido })
  const error = [ocupacion, ingresos, canales, indicadores].find((c) => c.isError)?.error

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Encabezado titulo="Reportes" subtitulo="Todos los reportes incluyen ambas fechas del periodo." />
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ alignItems: { md: 'center' } }}>
            <CampoFecha label="Desde" value={desde} onChange={(e) => setDesde(e.target.value)} sx={{ maxWidth: 200 }} />
            <CampoFecha label="Hasta" value={hasta} onChange={(e) => setHasta(e.target.value)} sx={{ maxWidth: 200 }}
                        error={!valido} helperText={!valido ? 'La fecha final debe ser igual o posterior a la inicial' : undefined} />
            <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
              {PERIODOS.map((p) => (
                <Button key={p.texto} size="small" variant="outlined"
                        onClick={() => { const [d, h] = p.rango(); setDesde(d); setHasta(h) }}>{p.texto}</Button>
              ))}
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      {error ? <MensajeError error={error} /> : null}
      {valido && indicadores.isLoading && <Cargando />}

      {ocupacion.data && ingresos.data && indicadores.data && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Indicador titulo="Ocupación" valor={porcentaje(ocupacion.data.porcentaje)}
                     detalle={`${ocupacion.data.nochesVendidas} de ${ocupacion.data.nochesDisponibles} noches`} destacado />
          <Indicador titulo="Ingresos (pagos)" valor={formatoPesos(ingresos.data.total)} detalle={`${ingresos.data.cantidadPagos} movimientos`} destacado />
          <Indicador titulo="Tarifa promedio por noche" valor={formatoPesos(indicadores.data.tarifaPromedioNoche)} detalle="Noches vendidas" />
          <Indicador titulo="Ingreso por noche disponible" valor={formatoPesos(indicadores.data.ingresoPorNocheDisponible)}
                     detalle={`Alojamiento: ${formatoPesos(indicadores.data.ingresoAlojamiento)}`} />
          <Indicador titulo="Tasa de no-show" valor={porcentaje(indicadores.data.tasaNoShow)} detalle="Sobre las llegadas en firme" />
          <Indicador titulo="Tasa de cancelación" valor={porcentaje(indicadores.data.tasaCancelacion)} detalle="Reservas creadas en el periodo" />
          <Indicador titulo="Antelación promedio" valor={`${indicadores.data.antelacionPromedioDias.toLocaleString('es-CO')} días`}
                     detalle="Entre la reserva y la llegada" />
          <Indicador titulo="Apartamentos a la venta" valor={String(ocupacion.data.apartamentosActivos)} detalle="Base de las noches disponibles" />
        </Grid>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 6 }}>
          <Seccion titulo="Ocupación por apartamento" tipo="ocupacion" desde={desde} hasta={hasta}
                   nota="Noches vendidas: reservas confirmadas, en curso o finalizadas. Las pendientes, canceladas y no-show no ocupan.">
            {ocupacion.data && (
              <Stack spacing={1.5}>
                {ocupacion.data.porApartamento.map((a) => (
                  <Box key={a.codigoApartamento}>
                    <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
                      <Typography sx={{ fontWeight: 600 }}>{a.codigoApartamento}</Typography>
                      <Typography color="text.secondary">{a.nochesVendidas} noches · {porcentaje(a.porcentaje)}</Typography>
                    </Stack>
                    <LinearProgress variant="determinate" value={Math.min(a.porcentaje, 100)}
                                    sx={{ height: 8, borderRadius: 4, bgcolor: colores.cafetalClaro }} />
                  </Box>
                ))}
              </Stack>
            )}
          </Seccion>
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <Seccion titulo="Ingresos por medio de pago" tipo="ingresos" desde={desde} hasta={hasta}
                   nota="Suma de los pagos registrados en el periodo; los reversos restan. Los cargos sin pagar no son ingreso.">
            {ingresos.data && (ingresos.data.porMedio.length === 0
              ? <Typography color="text.secondary">No hay pagos en el periodo.</Typography>
              : (
                <TableContainer>
                  <Table size="small">
                    <TableHead><TableRow><TableCell>Medio</TableCell><TableCell align="right">Movimientos</TableCell><TableCell align="right">Total</TableCell></TableRow></TableHead>
                    <TableBody>
                      {ingresos.data.porMedio.map((m) => (
                        <TableRow key={m.medio}>
                          <TableCell>{m.medio}</TableCell><TableCell align="right">{m.pagos}</TableCell>
                          <TableCell align="right">{formatoPesos(m.total)}</TableCell>
                        </TableRow>
                      ))}
                      <TableRow>
                        <TableCell sx={{ fontWeight: 700 }}>Total</TableCell><TableCell align="right">{ingresos.data.cantidadPagos}</TableCell>
                        <TableCell align="right" sx={{ fontWeight: 700 }}>{formatoPesos(ingresos.data.total)}</TableCell>
                      </TableRow>
                    </TableBody>
                  </Table>
                </TableContainer>
              ))}
          </Seccion>
        </Grid>
        <Grid size={12}>
          <Seccion titulo="Reservas por canal de origen" tipo="canales" desde={desde} hasta={hasta}
                   nota="Reservas creadas en el periodo, en su estado actual, incluidas las canceladas y los no-show.">
            {canales.data && (
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Canal</TableCell>
                      {ESTADOS_RESERVA.map((e: EstadoReserva) => <TableCell key={e} align="right">{estiloReserva[e].texto}</TableCell>)}
                      <TableCell align="right">Total</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {canales.data.canales.map((c) => (
                      <TableRow key={c.canal}>
                        <TableCell sx={{ fontWeight: 600 }}>{nombreCanal[c.canal]}</TableCell>
                        {ESTADOS_RESERVA.map((e) => <TableCell key={e} align="right">{c.porEstado[e] ?? 0}</TableCell>)}
                        <TableCell align="right" sx={{ fontWeight: 700 }}>{c.total}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Seccion>
        </Grid>
      </Grid>
      {valido && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
          Periodo: {formatoFecha(desde)} a {formatoFecha(hasta)}. Los indicadores propios también se descargan en CSV:{' '}
          <BotonCsv tipo="indicadores" desde={desde} hasta={hasta} />
        </Typography>
      )}
    </Container>
  )
}

function Indicador({ titulo, valor, detalle, destacado }: { titulo: string; valor: string; detalle: string; destacado?: boolean }) {
  return (
    <Grid size={{ xs: 6, md: 3 }}>
      <Card sx={{ height: '100%', ...(destacado ? { bgcolor: colores.cafetal, color: '#fff' } : {}) }}>
        <CardContent>
          <Typography variant="body2" sx={{ opacity: 0.8 }}>{titulo}</Typography>
          <Typography sx={{ fontSize: 26, fontWeight: 700, my: 0.5 }}>{valor}</Typography>
          <Typography variant="caption" sx={{ opacity: 0.75 }}>{detalle}</Typography>
        </CardContent>
      </Card>
    </Grid>
  )
}

function Seccion({ titulo, tipo, desde, hasta, nota, children }: {
  titulo: string; tipo: TipoReporte; desde: string; hasta: string; nota: string; children: ReactNode
}) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="h6">{titulo}</Typography>
          <BotonCsv tipo={tipo} desde={desde} hasta={hasta} />
        </Stack>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{nota}</Typography>
        {children}
      </CardContent>
    </Card>
  )
}

function BotonCsv({ tipo, desde, hasta }: { tipo: TipoReporte; desde: string; hasta: string }) {
  const avisos = useAvisos()
  const [descargando, setDescargando] = useState(false)
  const descargar = async () => {
    setDescargando(true)
    try {
      await reportes.descargarCsv(tipo, desde, hasta)
    } catch (e) {
      avisos.error(e)
    } finally {
      setDescargando(false)
    }
  }
  return (
    <Button size="small" startIcon={<DescargarIcon />} loading={descargando} disabled={!desde || !hasta || desde > hasta}
            onClick={descargar}>CSV</Button>
  )
}

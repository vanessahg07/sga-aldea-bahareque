import { useState } from 'react'
import { Link as RouterLink, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Box, Button, Card, CardContent, CardHeader, Chip, Container, Divider, Grid, IconButton, Link, Stack, Table,
  TableBody, TableCell, TableHead, TableRow, Tooltip, Typography,
} from '@mui/material'
import ReversoIcon from '@mui/icons-material/UndoRounded'
import { alojamiento, folios, reservas, type DatosEstancia } from '../../api/servicios'
import type { Folio } from '../../api/tipos'
import { useSesion } from '../../sesion/Sesion'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, CampoHora, Confirmacion, Dato, EstadoReservaChip, MensajeError } from '../../componentes/Comunes'
import { DesgloseCotizacion } from '../../componentes/Negocio'
import { colores } from '../../tema/tema'
import { nombreCanal, nombreCargo } from '../../utilidades/estados'
import { formatoFecha, formatoFechaHora, formatoHora, formatoPesos, hoyIso } from '../../utilidades/formato'
import { DialogoAjuste, DialogoMotivo, DialogoPago, DialogoServicio } from './DialogosFolio'
import { DialogoModificar } from './DialogoModificar'

/** Mutación que, al terminar bien, refresca la reserva y su folio y muestra el mensaje. */
function useAccion<T>(alExito: () => void, ejecutar: (valor: T) => Promise<unknown>, mensaje: string) {
  const avisos = useAvisos()
  return useMutation({
    mutationFn: ejecutar,
    onSuccess: () => { alExito(); avisos.exito(mensaje) },
    onError: avisos.error,
  })
}

type Dialogo = null | 'confirmar' | 'cancelar' | 'noShow' | 'llegada' | 'salida' | 'modificar' | 'hora'
  | 'pago' | 'servicio' | 'ajuste' | 'cierre' | { reversoCargo: string } | { reversoPago: string }

export function DetalleReserva() {
  const { id = '' } = useParams()
  const { usuario, tieneRol } = useSesion()
  const clienteConsultas = useQueryClient()
  const [dialogo, setDialogo] = useState<Dialogo>(null)
  const [hora, setHora] = useState('')

  const personal = tieneRol('RECEPCIONISTA', 'ADMINISTRADOR')
  const esAdmin = tieneRol('ADMINISTRADOR')
  const reserva = useQuery({ queryKey: ['reserva', id], queryFn: () => reservas.consultar(id) })
  const folio = useQuery({ queryKey: ['folio', id], queryFn: () => folios.consultar(id) })
  const datosAlojamiento = useQuery({ queryKey: ['alojamiento'], queryFn: alojamiento.consultar })

  const refrescar = () => {
    setDialogo(null)
    clienteConsultas.invalidateQueries({ queryKey: ['reserva', id] })
    clienteConsultas.invalidateQueries({ queryKey: ['folio', id] })
    clienteConsultas.invalidateQueries({ queryKey: ['reservas'] })
  }

  const confirmar = useAccion(refrescar, () => reservas.confirmar(id), 'Reserva confirmada')
  const cancelar = useAccion(refrescar, () => reservas.cancelar(id), 'Reserva cancelada; las noches quedaron libres')
  const noShow = useAccion(refrescar, () => reservas.declararNoShow(id), 'Se declaró el no-show')
  const llegada = useAccion(refrescar, () => reservas.registrarLlegada(id), 'Llegada registrada: el apartamento quedó ocupado')
  const salida = useAccion(refrescar, (motivo: string) => reservas.registrarSalida(id, motivo), 'Salida registrada: el apartamento quedó pendiente de preparación')
  const modificar = useAccion(refrescar, (datos: DatosEstancia) => reservas.modificar(id, datos), 'Reserva modificada')
  const guardarHora = useAccion(refrescar, (h: string) => reservas.registrarHoraLlegada(id, h), 'Hora de llegada registrada')
  const pago = useAccion(refrescar, (p: { valor: number; medio: string }) => folios.registrarPago(id, p.valor, p.medio), 'Pago registrado')
  const servicio = useAccion(refrescar, (s: { codigo: string; cantidad: number }) => folios.cargarServicio(id, s.codigo, s.cantidad), 'Servicio cargado al folio')
  const ajuste = useAccion(refrescar, (a: { valor: number; descripcion: string }) => folios.registrarAjuste(id, a.valor, a.descripcion), 'Ajuste registrado')
  const reversoCargo = useAccion(refrescar, (r: { id: string; motivo: string }) => folios.revertirCargo(id, r.id, r.motivo), 'Cargo revertido')
  const reversoPago = useAccion(refrescar, (idPago: string) => folios.revertirPago(id, idPago), 'Pago revertido')
  const cierre = useAccion(refrescar, (motivo: string) => folios.cerrar(id, motivo), 'Folio cerrado')

  if (reserva.isLoading) return <Cargando />
  if (reserva.isError) return <Container sx={{ py: 4 }}><MensajeError error={reserva.error} /></Container>
  const r = reserva.data!
  const f = folio.data
  const activa = r.estado === 'PENDIENTE' || r.estado === 'CONFIRMADA'
  const llegoElDia = hoyIso() >= r.entrada
  const saldo = f?.saldo ?? r.saldo
  const aloj = datosAlojamiento.data

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ justifyContent: 'space-between', mb: 3 }}>
        <Box>
          <Link component={RouterLink} to={personal ? '/recepcion/reservas' : '/mis-reservas'} variant="body2">← Volver a reservas</Link>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', mt: 1, flexWrap: 'wrap' }}>
            <Typography variant="h3" component="h1">
              <Link component={RouterLink} to={`/apartamentos/${r.codigoApartamento}`} color="inherit" underline="hover">
                {r.codigoApartamento}
              </Link>{' · '}{formatoFecha(r.entrada)} → {formatoFecha(r.salida)}
            </Typography>
            <EstadoReservaChip estado={r.estado} />
          </Stack>
          <Typography color="text.secondary" sx={{ mt: 0.5 }}>
            Canal {nombreCanal[r.canal].toLowerCase()}{r.referenciaExterna ? ` (${r.referenciaExterna})` : ''} · creada el {formatoFechaHora(r.creadaEn)}
          </Typography>
        </Box>
        <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1, alignItems: 'flex-start' }}>
          {personal && r.estado === 'PENDIENTE' && <Button variant="contained" onClick={() => setDialogo('confirmar')}>Confirmar</Button>}
          {personal && r.estado === 'CONFIRMADA' && llegoElDia &&
            <Button variant="contained" onClick={() => setDialogo('llegada')}>Registrar llegada</Button>}
          {personal && r.estado === 'EN_CURSO' && <Button variant="contained" onClick={() => setDialogo('salida')}>Registrar salida</Button>}
          {personal && activa && <Button variant="outlined" onClick={() => setDialogo('modificar')}>Modificar</Button>}
          {!personal && activa && <Button variant="outlined" onClick={() => { setHora(r.horaEstimadaLlegada?.slice(0, 5) ?? ''); setDialogo('hora') }}>Hora de llegada</Button>}
          {personal && r.estado === 'CONFIRMADA' && llegoElDia &&
            <Button variant="outlined" color="secondary" onClick={() => setDialogo('noShow')}>No-show</Button>}
          {activa && <Button variant="outlined" color="secondary" onClick={() => setDialogo('cancelar')}>Cancelar reserva</Button>}
        </Stack>
      </Stack>

      {r.estado === 'PENDIENTE' && aloj && (
        <Alert severity="warning" sx={{ mb: 3 }}>
          La reserva se confirma con el pago del anticipo ({aloj.parametros.porcentajeAnticipo}%:{' '}
          {formatoPesos(Math.round(r.cotizacion.valorTotal * aloj.parametros.porcentajeAnticipo / 100))})
          {!r.horaEstimadaLlegada && ' y la hora estimada de llegada'}. Si no se confirma en {aloj.parametros.horasPlazoConfirmacion} horas
          desde su creación, se cancela automáticamente.
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Stack spacing={3}>
            <Card>
              <CardHeader title="Estancia y grupo" slotProps={{ title: { variant: 'h5' } }} />
              <CardContent sx={{ pt: 0 }}>
                <Grid container spacing={2}>
                  <Grid size={6}><Dato etiqueta="Noches">{r.noches}</Dato></Grid>
                  <Grid size={6}><Dato etiqueta="Hora estimada de llegada">{formatoHora(r.horaEstimadaLlegada)}</Dato></Grid>
                  <Grid size={6}><Dato etiqueta="Titular">{r.titular.nombre}</Dato></Grid>
                  <Grid size={6}><Dato etiqueta="Documento">{r.titular.documento}</Dato></Grid>
                  <Grid size={6}><Dato etiqueta="Correo">{r.titular.correo ?? '—'}</Dato></Grid>
                  <Grid size={6}><Dato etiqueta="Teléfono">{r.titular.telefono ?? '—'}</Dato></Grid>
                  <Grid size={6}><Dato etiqueta="Ocupantes">{r.totalOcupantes}</Dato></Grid>
                  <Grid size={6}><Dato etiqueta="Mascotas">{r.mascotas}</Dato></Grid>
                </Grid>
                {r.acompanantes.length > 0 && (
                  <>
                    <Divider sx={{ my: 2 }} />
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Acompañantes</Typography>
                    <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
                      {r.acompanantes.map((o, i) => <Chip key={i} label={`${o.nombre} · ${formatoFecha(o.fechaNacimiento)}`} variant="outlined" />)}
                    </Stack>
                  </>
                )}
              </CardContent>
            </Card>
            <Card>
              <CardHeader title="Valor congelado de la estancia" slotProps={{ title: { variant: 'h5' } }}
                          subheader={`Política de cancelación versión ${r.versionPolitica}`} />
              <DesgloseCotizacion cotizacion={r.cotizacion} />
              <Box sx={{ pb: 2 }} />
            </Card>
          </Stack>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardHeader title="Folio" slotProps={{ title: { variant: 'h5' } }}
                        action={f?.cerrado ? <Chip label="Cerrado" size="small" /> : <Chip label="Abierto" size="small" color="primary" variant="outlined" />} />
            <CardContent sx={{ pt: 0 }}>
              {folio.isError && <MensajeError error={folio.error} />}
              {f && <ContenidoFolio folio={f} personal={personal} alRevertirCargo={(c) => setDialogo({ reversoCargo: c })}
                                    alRevertirPago={(p) => setDialogo({ reversoPago: p })} />}
              {personal && f && !f.cerrado && (
                <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1, mt: 2 }}>
                  <Button variant="contained" color="secondary" onClick={() => setDialogo('pago')}>Registrar pago</Button>
                  <Button variant="outlined" onClick={() => setDialogo('servicio')}>Cargar servicio</Button>
                  <Button variant="outlined" onClick={() => setDialogo('ajuste')}>Ajuste</Button>
                  {(r.estado === 'FINALIZADA' || r.estado === 'CANCELADA' || r.estado === 'NO_SHOW') &&
                    <Button variant="outlined" onClick={() => setDialogo('cierre')}>Cerrar folio</Button>}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Confirmacion abierto={dialogo === 'confirmar'} titulo="Confirmar reserva" cargando={confirmar.isPending}
                    alCerrar={() => setDialogo(null)} alConfirmar={() => confirmar.mutate(undefined)}>
        Se verificará que la reserva tenga la hora estimada de llegada y el anticipo pagado.
      </Confirmacion>
      <Confirmacion abierto={dialogo === 'cancelar'} titulo="Cancelar reserva" peligrosa textoConfirmar="Cancelar reserva"
                    cargando={cancelar.isPending} alCerrar={() => setDialogo(null)} alConfirmar={() => cancelar.mutate(undefined)}>
        Se aplicará la política de cancelación con la que se creó la reserva (versión {r.versionPolitica}). La retención
        queda como penalidad en el folio y lo pagado de más, como saldo a favor. <Link component={RouterLink} to="/politica">Ver política</Link>
      </Confirmacion>
      <Confirmacion abierto={dialogo === 'noShow'} titulo="Declarar no-show" peligrosa textoConfirmar="Declarar no-show"
                    cargando={noShow.isPending} alCerrar={() => setDialogo(null)} alConfirmar={() => noShow.mutate(undefined)}>
        Solo procede desde la hora límite del día de entrada{aloj ? ` (${formatoHora(aloj.parametros.horaLimiteNoShow)})` : ''}.
        Se aplica la retención de la política y se liberan las noches.
      </Confirmacion>
      <Confirmacion abierto={dialogo === 'llegada'} titulo="Registrar llegada" cargando={llegada.isPending}
                    alCerrar={() => setDialogo(null)} alConfirmar={() => llegada.mutate(undefined)}>
        El apartamento {r.codigoApartamento} debe estar preparado. Quedará ocupado por el grupo.
      </Confirmacion>
      {dialogo === 'salida' && (saldo === 0 || !esAdmin) && (
        <Confirmacion abierto titulo="Registrar salida" cargando={salida.isPending} alCerrar={() => setDialogo(null)}
                      alConfirmar={() => salida.mutate('')}>
          {saldo === 0 ? 'El folio está a paz y salvo y se cerrará con la salida.'
            : `El folio tiene saldo de ${formatoPesos(saldo)}. Registre el pago antes de la salida o pida al administrador que autorice el cierre.`}
        </Confirmacion>
      )}
      <DialogoMotivo abierto={dialogo === 'salida' && saldo !== 0 && esAdmin} cargando={salida.isPending}
                     alCerrar={() => setDialogo(null)} titulo="Salida con saldo pendiente" textoAceptar="Autorizar y registrar salida"
                     etiquetaMotivo="Motivo de la autorización" alAceptar={(m) => salida.mutate(m)}
                     descripcion={`El folio tiene saldo de ${formatoPesos(saldo)}. Como administrador puede autorizar el cierre; quedará registrado con su nombre y el motivo.`} />
      <DialogoMotivo abierto={dialogo === 'cierre'} cargando={cierre.isPending} alCerrar={() => setDialogo(null)}
                     titulo="Cerrar folio" textoAceptar="Cerrar folio" etiquetaMotivo="Motivo de la autorización (solo con saldo)"
                     obligatorio={saldo !== 0} alAceptar={(m) => cierre.mutate(m)}
                     descripcion={saldo === 0 ? 'El saldo es cero; el folio se cerrará.'
                       : `El saldo es ${formatoPesos(saldo)}. Solo el administrador puede cerrarlo, indicando el motivo.`} />
      <DialogoMotivo abierto={typeof dialogo === 'object' && dialogo !== null && 'reversoCargo' in dialogo}
                     cargando={reversoCargo.isPending} alCerrar={() => setDialogo(null)} titulo="Revertir cargo"
                     textoAceptar="Revertir" etiquetaMotivo="Motivo de la corrección"
                     descripcion="Los cargos no se editan ni se borran: se registra un cargo inverso."
                     alAceptar={(m) => dialogo && typeof dialogo === 'object' && 'reversoCargo' in dialogo && reversoCargo.mutate({ id: dialogo.reversoCargo, motivo: m })} />
      <Confirmacion abierto={typeof dialogo === 'object' && dialogo !== null && 'reversoPago' in dialogo} titulo="Revertir pago"
                    cargando={reversoPago.isPending} alCerrar={() => setDialogo(null)}
                    alConfirmar={() => dialogo && typeof dialogo === 'object' && 'reversoPago' in dialogo && reversoPago.mutate(dialogo.reversoPago)}>
        Se registrará un pago inverso por el mismo valor. El pago original se conserva en el folio.
      </Confirmacion>
      {f && aloj && (
        <>
          <DialogoPago abierto={dialogo === 'pago'} cargando={pago.isPending} alCerrar={() => setDialogo(null)} saldo={f.saldo}
                       mediosPago={aloj.mediosPago} alRegistrar={(valor, medio) => pago.mutate({ valor, medio })} />
          <DialogoServicio abierto={dialogo === 'servicio'} cargando={servicio.isPending} alCerrar={() => setDialogo(null)}
                           alojamiento={aloj} alCargar={(codigo, cantidad) => servicio.mutate({ codigo, cantidad })} />
          <DialogoAjuste abierto={dialogo === 'ajuste'} cargando={ajuste.isPending} alCerrar={() => setDialogo(null)}
                         alRegistrar={(valor, descripcion) => ajuste.mutate({ valor, descripcion })} />
        </>
      )}
      <DialogoModificar abierto={dialogo === 'modificar'} reserva={r} cargando={modificar.isPending}
                        alCerrar={() => setDialogo(null)} alGuardar={(d) => modificar.mutate(d)} />
      <Confirmacion abierto={dialogo === 'hora'} titulo="Hora estimada de llegada" cargando={guardarHora.isPending}
                    textoConfirmar="Guardar" alCerrar={() => setDialogo(null)} alConfirmar={() => hora && guardarHora.mutate(hora)}>
        <Box sx={{ pt: 1 }}><CampoHora label="Hora" value={hora} onChange={(e) => setHora(e.target.value)} /></Box>
      </Confirmacion>
      {usuario && !personal && r.estado === 'CANCELADA' && saldo < 0 && (
        <Alert severity="info" sx={{ mt: 3 }}>Tiene un saldo a favor de {formatoPesos(-saldo)}. El alojamiento le contactará para la devolución.</Alert>
      )}
    </Container>
  )
}

function ContenidoFolio({ folio, personal, alRevertirCargo, alRevertirPago }: {
  folio: Folio
  personal: boolean
  alRevertirCargo: (id: string) => void
  alRevertirPago: (id: string) => void
}) {
  const revertidos = new Set([...folio.cargos.map((c) => c.idCargoRevertido), ...folio.pagos.map((p) => p.idPagoRevertido)])
  const puedeRevertir = personal && !folio.cerrado
  return (
    <Stack spacing={2}>
      <Box sx={{ p: 2, borderRadius: 2, bgcolor: folio.saldo > 0 ? colores.guaduaClara : colores.cafetalClaro }}>
        <Typography variant="body2" color="text.secondary">
          {folio.saldo > 0 ? 'Saldo pendiente' : folio.saldo < 0 ? 'Saldo a favor del huésped' : 'A paz y salvo'}
        </Typography>
        <Typography variant="h3">{formatoPesos(Math.abs(folio.saldo))}</Typography>
        <Typography variant="body2" color="text.secondary">
          Cargos {formatoPesos(folio.totalCargos)} · Pagos {formatoPesos(folio.totalPagos)}
        </Typography>
      </Box>
      <Box>
        <Typography variant="h6" sx={{ mb: 1 }}>Cargos</Typography>
        <Table size="small">
          <TableHead><TableRow><TableCell>Fecha</TableCell><TableCell>Concepto</TableCell><TableCell align="right">Valor</TableCell>{puedeRevertir && <TableCell />}</TableRow></TableHead>
          <TableBody>
            {folio.cargos.map((c) => (
              <TableRow key={c.id} sx={{ opacity: revertidos.has(c.id) || c.idCargoRevertido ? 0.6 : 1 }}>
                <TableCell>{formatoFecha(c.fecha)}</TableCell>
                <TableCell>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>{nombreCargo[c.tipo]}</Typography>
                  <Typography variant="caption" color="text.secondary">{c.descripcion}</Typography>
                </TableCell>
                <TableCell align="right">{formatoPesos(c.valor)}</TableCell>
                {puedeRevertir && (
                  <TableCell align="right">
                    {!c.idCargoRevertido && !revertidos.has(c.id) && (
                      <Tooltip title="Revertir cargo"><IconButton size="small" onClick={() => alRevertirCargo(c.id)}><ReversoIcon fontSize="small" /></IconButton></Tooltip>
                    )}
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>
      <Box>
        <Typography variant="h6" sx={{ mb: 1 }}>Pagos</Typography>
        {folio.pagos.length === 0 ? <Typography color="text.secondary" variant="body2">Aún no hay pagos registrados.</Typography> : (
          <Table size="small">
            <TableHead><TableRow><TableCell>Fecha</TableCell><TableCell>Medio</TableCell><TableCell align="right">Valor</TableCell>{puedeRevertir && <TableCell />}</TableRow></TableHead>
            <TableBody>
              {folio.pagos.map((p) => (
                <TableRow key={p.id} sx={{ opacity: revertidos.has(p.id) || p.idPagoRevertido ? 0.6 : 1 }}>
                  <TableCell>{formatoFecha(p.fecha)}</TableCell>
                  <TableCell>{p.idPagoRevertido ? `Reverso · ${p.medio.toLowerCase()}` : p.medio.charAt(0) + p.medio.slice(1).toLowerCase()}</TableCell>
                  <TableCell align="right">{formatoPesos(p.valor)}</TableCell>
                  {puedeRevertir && (
                    <TableCell align="right">
                      {!p.idPagoRevertido && !revertidos.has(p.id) && (
                        <Tooltip title="Revertir pago"><IconButton size="small" onClick={() => alRevertirPago(p.id)}><ReversoIcon fontSize="small" /></IconButton></Tooltip>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Box>
      {folio.autorizacionCierre && (
        <Alert severity="info">
          Cierre autorizado por {folio.autorizacionCierre.autor} el {formatoFechaHora(folio.autorizacionCierre.fecha)}: {folio.autorizacionCierre.motivo}
        </Alert>
      )}
    </Stack>
  )
}


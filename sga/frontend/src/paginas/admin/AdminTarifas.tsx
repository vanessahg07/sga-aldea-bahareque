import { useState } from 'react'
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Box, Button, Card, CardContent, Chip, Container, Dialog, DialogActions, DialogContent, DialogTitle, Grid,
  IconButton, InputAdornment, Stack, Tab, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Tabs,
  TextField, Tooltip, Typography,
} from '@mui/material'
import EditarIcon from '@mui/icons-material/EditOutlined'
import QuitarIcon from '@mui/icons-material/CloseRounded'
import { apartamentos, tarifas } from '../../api/servicios'
import type { Rango, Temporada, Tramo } from '../../api/tipos'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, CampoFecha, Confirmacion, Encabezado, MensajeError } from '../../componentes/Comunes'
import { etiqueta, formatoFecha, formatoFechaHora, formatoPesos } from '../../utilidades/formato'

export function AdminTarifas() {
  const [pestana, setPestana] = useState(0)
  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Encabezado titulo="Tarifas y políticas" subtitulo="Todo cambio aplica a las reservas nuevas; las existentes conservan su valor y su política." />
      <Tabs value={pestana} onChange={(_, v) => setPestana(v)} sx={{ mb: 3, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="Tarifas" /><Tab label="Temporadas" /><Tab label="Política de cancelación" />
      </Tabs>
      {pestana === 0 && <MatrizTarifas />}
      {pestana === 1 && <Temporadas />}
      {pestana === 2 && <Politicas />}
    </Container>
  )
}

// --- Tarifas por apartamento y temporada ---

function MatrizTarifas() {
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const inventario = useQuery({ queryKey: ['apartamentos', 'gestion', 0], queryFn: () => apartamentos.gestion(0) })
  const temporadas = useQuery({ queryKey: ['temporadas'], queryFn: tarifas.temporadas })
  const incompletas = useQuery({ queryKey: ['tarifas', 'incompletas'], queryFn: tarifas.incompletas })
  const codigos = inventario.data?.contenido.map((a) => a.codigo) ?? []
  const vigentes = useQueries({ queries: codigos.map((c) => ({ queryKey: ['tarifas', c], queryFn: () => apartamentos.tarifas(c) })) })
  const [edicion, setEdicion] = useState<{ codigo: string; temporada: string; valor: string } | null>(null)

  const registrar = useMutation({
    mutationFn: () => tarifas.registrar(edicion!.codigo, edicion!.temporada, Number(edicion!.valor)),
    onSuccess: (t) => {
      avisos.exito(`Nueva tarifa de ${t.codigoApartamento} en ${etiqueta(t.temporada)}: ${formatoPesos(t.valor)}`)
      setEdicion(null)
      clienteConsultas.invalidateQueries({ queryKey: ['tarifas'] })
    },
    onError: avisos.error,
  })

  if (inventario.isLoading || temporadas.isLoading) return <Cargando />
  if (inventario.isError) return <MensajeError error={inventario.error} />
  return (
    <>
      {incompletas.data && incompletas.data.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Apartamentos activos sin tarifa en alguna temporada (no se pueden vender): {incompletas.data.join(', ')}
        </Alert>
      )}
      <Card>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Apartamento</TableCell>
                {temporadas.data?.map((t) => <TableCell key={t.nombre} align="right">{etiqueta(t.nombre)}</TableCell>)}
              </TableRow>
            </TableHead>
            <TableBody>
              {inventario.data!.contenido.map((a, i) => (
                <TableRow key={a.codigo}>
                  <TableCell><Typography sx={{ fontWeight: 600 }}>{a.codigo}</Typography><Typography variant="caption" color="text.secondary">{a.nombre}</Typography></TableCell>
                  {temporadas.data?.map((t) => {
                    const tarifa = vigentes[i]?.data?.find((x) => x.temporada === t.nombre)
                    return (
                      <TableCell key={t.nombre} align="right">
                        <Stack direction="row" spacing={0.5} sx={{ justifyContent: 'flex-end', alignItems: 'center' }}>
                          {tarifa ? <span>{formatoPesos(tarifa.valor)}</span>
                            : vigentes[i]?.isPending ? <span>—</span>
                              : <Chip size="small" label="Sin tarifa" color="secondary" variant="outlined" />}
                          <Tooltip title="Registrar nueva tarifa">
                            <IconButton size="small" onClick={() => setEdicion({ codigo: a.codigo, temporada: t.nombre, valor: tarifa ? String(tarifa.valor) : '' })}>
                              <EditarIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                      </TableCell>
                    )
                  })}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 1.5 }}>
        Valor por ocupante facturable, por noche. Una nueva tarifa rige desde el momento en que se registra y la anterior queda en el histórico.
      </Typography>
      <Dialog open={!!edicion} onClose={() => setEdicion(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Tarifa de {edicion?.codigo} en {edicion && etiqueta(edicion.temporada)}</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 1 }}>
            <TextField label="Valor por persona y noche" type="number" value={edicion?.valor ?? ''} autoFocus
                       onChange={(e) => edicion && setEdicion({ ...edicion, valor: e.target.value })}
                       slotProps={{ input: { startAdornment: <InputAdornment position="start">$</InputAdornment> } }} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button color="inherit" onClick={() => setEdicion(null)}>Cancelar</Button>
          <Button variant="contained" loading={registrar.isPending} disabled={!(Number(edicion?.valor) > 0)} onClick={() => registrar.mutate()}>
            Registrar
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}

// --- Temporadas ---

interface Borrador { nombre: string; rangos: Rango[]; estanciaMinimaNoches: number; esBase: boolean; nueva: boolean }

function Temporadas() {
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const consulta = useQuery({ queryKey: ['temporadas'], queryFn: tarifas.temporadas })
  const [borrador, setBorrador] = useState<Borrador | null>(null)
  const [eliminar, setEliminar] = useState<Temporada | null>(null)

  const alTerminar = (mensaje: string) => () => {
    avisos.exito(mensaje)
    setBorrador(null)
    setEliminar(null)
    clienteConsultas.invalidateQueries({ queryKey: ['temporadas'] })
    clienteConsultas.invalidateQueries({ queryKey: ['tarifas'] })
  }
  const guardar = useMutation({
    mutationFn: (b: Borrador) => b.nueva ? tarifas.crearTemporada(b.nombre, b.rangos, b.estanciaMinimaNoches)
      : tarifas.actualizarTemporada(b.nombre, b.rangos, b.estanciaMinimaNoches),
    onSuccess: alTerminar('Temporada guardada. Recuerde registrar las tarifas de cada apartamento.'),
    onError: avisos.error,
  })
  const borrar = useMutation({ mutationFn: (n: string) => tarifas.eliminarTemporada(n), onSuccess: alTerminar('Temporada retirada'), onError: avisos.error })

  if (consulta.isLoading) return <Cargando />
  if (consulta.isError) return <MensajeError error={consulta.error} />
  const actualizarRango = (i: number, cambio: Partial<Rango>) =>
    borrador && setBorrador({ ...borrador, rangos: borrador.rangos.map((r, j) => (j === i ? { ...r, ...cambio } : r)) })

  return (
    <>
      <Stack direction="row" sx={{ justifyContent: 'flex-end', mb: 2 }}>
        <Button variant="contained" color="secondary"
                onClick={() => setBorrador({ nombre: '', rangos: [{ inicio: '', fin: '' }], estanciaMinimaNoches: 1, esBase: false, nueva: true })}>
          Nueva temporada
        </Button>
      </Stack>
      <Grid container spacing={2}>
        {consulta.data!.map((t) => (
          <Grid key={t.nombre} size={{ xs: 12, md: 6 }}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Typography variant="h5">{etiqueta(t.nombre)}</Typography>
                    {t.esBase && <Chip size="small" label="Base" color="primary" />}
                  </Stack>
                  <Stack direction="row">
                    <Button size="small" onClick={() => setBorrador({ ...t, nueva: false })}>Editar</Button>
                    {!t.esBase && <Button size="small" color="secondary" onClick={() => setEliminar(t)}>Retirar</Button>}
                  </Stack>
                </Stack>
                <Typography color="text.secondary" sx={{ mt: 1 }}>
                  {t.esBase ? 'Cubre todas las fechas no asignadas a otra temporada.'
                    : t.rangos.map((r) => `${formatoFecha(r.inicio)} → ${formatoFecha(r.fin)}`).join(' · ')}
                </Typography>
                <Typography variant="body2" sx={{ mt: 1 }}>Estancia mínima: {t.estanciaMinimaNoches} {t.estanciaMinimaNoches === 1 ? 'noche' : 'noches'}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Dialog open={!!borrador} onClose={() => setBorrador(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{borrador?.nueva ? 'Nueva temporada' : `Editar ${borrador && etiqueta(borrador.nombre)}`}</DialogTitle>
        <DialogContent>
          {borrador && (
            <Stack spacing={2} sx={{ pt: 1 }}>
              {borrador.nueva && <TextField label="Nombre" value={borrador.nombre} placeholder="PUENTES_FESTIVOS"
                                            onChange={(e) => setBorrador({ ...borrador, nombre: e.target.value.toUpperCase().replace(/\s+/g, '_') })} />}
              <TextField label="Estancia mínima (noches)" type="number" value={borrador.estanciaMinimaNoches} slotProps={{ htmlInput: { min: 1 } }}
                         onChange={(e) => setBorrador({ ...borrador, estanciaMinimaNoches: Number(e.target.value) })} />
              {!borrador.esBase && (
                <>
                  <Typography variant="body2" color="text.secondary">Rangos (la fecha final no se incluye). No pueden solaparse con otras temporadas.</Typography>
                  {borrador.rangos.map((r, i) => (
                    <Stack key={i} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <CampoFecha label="Inicio" value={r.inicio} onChange={(e) => actualizarRango(i, { inicio: e.target.value })} />
                      <CampoFecha label="Fin" value={r.fin} onChange={(e) => actualizarRango(i, { fin: e.target.value })} />
                      <IconButton aria-label="Quitar rango" onClick={() => setBorrador({ ...borrador, rangos: borrador.rangos.filter((_, j) => j !== i) })}>
                        <QuitarIcon />
                      </IconButton>
                    </Stack>
                  ))}
                  <Box><Button size="small" onClick={() => setBorrador({ ...borrador, rangos: [...borrador.rangos, { inicio: '', fin: '' }] })}>Agregar rango</Button></Box>
                </>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button color="inherit" onClick={() => setBorrador(null)}>Cancelar</Button>
          <Button variant="contained" loading={guardar.isPending} onClick={() => borrador && guardar.mutate(borrador)}
                  disabled={!borrador?.nombre || (!borrador.esBase && borrador.rangos.some((r) => !r.inicio || !r.fin))}>
            Guardar
          </Button>
        </DialogActions>
      </Dialog>
      <Confirmacion abierto={!!eliminar} titulo={`Retirar ${eliminar ? etiqueta(eliminar.nombre) : ''}`} peligrosa textoConfirmar="Retirar"
                    cargando={borrar.isPending} alCerrar={() => setEliminar(null)} alConfirmar={() => eliminar && borrar.mutate(eliminar.nombre)}>
        Sus fechas pasarán a la temporada base. Las reservas existentes conservan su valor.
      </Confirmacion>
    </>
  )
}

// --- Política de cancelación ---

function Politicas() {
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const versiones = useQuery({ queryKey: ['politicas'], queryFn: tarifas.versionesPolitica })
  const [tramos, setTramos] = useState<Tramo[] | null>(null)
  const [noShow, setNoShow] = useState('')

  const publicar = useMutation({
    mutationFn: () => tarifas.publicarPolitica(tramos!, Number(noShow)),
    onSuccess: (p) => {
      avisos.exito(`Política versión ${p.version} publicada`)
      setTramos(null)
      clienteConsultas.invalidateQueries({ queryKey: ['politicas'] })
      clienteConsultas.invalidateQueries({ queryKey: ['politica'] })
    },
    onError: avisos.error,
  })

  if (versiones.isLoading) return <Cargando />
  if (versiones.isError) return <MensajeError error={versiones.error} />
  const lista = [...versiones.data!].reverse()
  const vigente = lista[0]

  return (
    <Grid container spacing={3}>
      <Grid size={{ xs: 12, md: 6 }}>
        <Card>
          <CardContent>
            <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Typography variant="h5">Nueva versión</Typography>
              {!tramos && vigente && (
                <Button size="small" onClick={() => { setTramos(vigente.tramos); setNoShow(String(vigente.porcentajeRetencionNoShow)) }}>
                  Partir de la vigente
                </Button>
              )}
            </Stack>
            {!tramos ? <Typography color="text.secondary">Publicar una versión nueva no altera las reservas ya creadas.</Typography> : (
              <Stack spacing={1.5}>
                {tramos.map((t, i) => (
                  <Stack key={i} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <TextField label="Días de antelación (mínimo)" type="number" value={t.diasAntelacionMinimos}
                               onChange={(e) => setTramos(tramos.map((x, j) => j === i ? { ...x, diasAntelacionMinimos: Number(e.target.value) } : x))} />
                    <TextField label="Retención" type="number" value={t.porcentajeRetencion}
                               slotProps={{ input: { endAdornment: <InputAdornment position="end">%</InputAdornment> } }}
                               onChange={(e) => setTramos(tramos.map((x, j) => j === i ? { ...x, porcentajeRetencion: Number(e.target.value) } : x))} />
                    <IconButton aria-label="Quitar tramo" onClick={() => setTramos(tramos.filter((_, j) => j !== i))}><QuitarIcon /></IconButton>
                  </Stack>
                ))}
                <Box><Button size="small" onClick={() => setTramos([...tramos, { diasAntelacionMinimos: 0, porcentajeRetencion: 0 }])}>Agregar tramo</Button></Box>
                <TextField label="Retención por no-show" type="number" value={noShow} onChange={(e) => setNoShow(e.target.value)}
                           slotProps={{ input: { endAdornment: <InputAdornment position="end">%</InputAdornment> } }} />
                <Typography variant="caption" color="text.secondary">Se exigen al menos dos tramos y uno debe cubrir 0 días de antelación.</Typography>
                <Stack direction="row" spacing={1}>
                  <Button variant="contained" loading={publicar.isPending} onClick={() => publicar.mutate()}>Publicar versión</Button>
                  <Button color="inherit" onClick={() => setTramos(null)}>Descartar</Button>
                </Stack>
              </Stack>
            )}
          </CardContent>
        </Card>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Stack spacing={2}>
          {lista.map((p, i) => (
            <Card key={p.version}>
              <CardContent>
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1 }}>
                  <Typography variant="h5">Versión {p.version}</Typography>
                  {i === 0 && <Chip size="small" label="Vigente" color="primary" />}
                </Stack>
                <Typography variant="body2" color="text.secondary">Desde {formatoFechaHora(p.vigenteDesde)}</Typography>
                <Table size="small" sx={{ mt: 1 }}>
                  <TableBody>
                    {p.tramos.map((t) => (
                      <TableRow key={t.diasAntelacionMinimos}>
                        <TableCell>{t.diasAntelacionMinimos} días o más</TableCell>
                        <TableCell align="right">{t.porcentajeRetencion}%</TableCell>
                      </TableRow>
                    ))}
                    <TableRow><TableCell>No-show</TableCell><TableCell align="right">{p.porcentajeRetencionNoShow}%</TableCell></TableRow>
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          ))}
        </Stack>
      </Grid>
    </Grid>
  )
}

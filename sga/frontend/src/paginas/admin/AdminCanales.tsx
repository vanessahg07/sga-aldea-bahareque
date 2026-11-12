import { useState } from 'react'
import { Link as RouterLink } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Box, Button, Card, Chip, Container, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel,
  Grid, IconButton, Link, MenuItem, Stack, Switch, Tab, Table, TableBody, TableCell, TableContainer, TableHead,
  TableRow, Tabs, TextField, Tooltip, Typography,
} from '@mui/material'
import CopiarIcon from '@mui/icons-material/ContentCopyOutlined'
import { canales } from '../../api/servicios'
import type { CanalVenta, ConflictoCanal, CredencialCanal, TipoCanal } from '../../api/tipos'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, Encabezado, MensajeError, Paginador, Vacio } from '../../componentes/Comunes'
import { estiloEventoCanal, nombreTipoCanal, TIPOS_CANAL } from '../../utilidades/estados'
import { formatoFecha, formatoFechaHora } from '../../utilidades/formato'
import { colores } from '../../tema/tema'

/** Canales de venta externos, conflictos pendientes de revisión (RN-18) y bitácora de eventos. */
export function AdminCanales() {
  const [pestana, setPestana] = useState(0)
  const pendientes = useQuery({ queryKey: ['conflictos', true, 0], queryFn: () => canales.conflictos(true, 0) })
  const cantidadPendientes = pendientes.data?.totalElementos ?? 0

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Encabezado titulo="Canales de venta"
                  subtitulo="Agencias y plataformas que consultan disponibilidad y envían reservas por la API del canal externo." />
      <Tabs value={pestana} onChange={(_, v) => setPestana(v)} sx={{ mb: 3, borderBottom: 1, borderColor: 'divider' }} variant="scrollable">
        <Tab label="Canales" />
        <Tab label={cantidadPendientes > 0 ? `Conflictos (${cantidadPendientes} ${cantidadPendientes === 1 ? 'pendiente' : 'pendientes'})` : 'Conflictos'} />
        <Tab label="Bitácora" />
      </Tabs>
      {pestana === 0 && <ListaCanales />}
      {pestana === 1 && <Conflictos />}
      {pestana === 2 && <Bitacora />}
    </Container>
  )
}

// --- Canales ---

function ListaCanales() {
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const consulta = useQuery({ queryKey: ['canales'], queryFn: canales.listar })
  const [nuevo, setNuevo] = useState(false)
  const [credencial, setCredencial] = useState<CredencialCanal | null>(null)

  const alObtenerCredencial = (resultado: CredencialCanal) => {
    setNuevo(false)
    setCredencial(resultado)
    clienteConsultas.invalidateQueries({ queryKey: ['canales'] })
  }
  const renovar = useMutation({ mutationFn: canales.renovarCredencial, onSuccess: alObtenerCredencial, onError: avisos.error })
  const activar = useMutation({
    mutationFn: ({ codigo, activo }: { codigo: string; activo: boolean }) => canales.cambiarActivo(codigo, activo),
    onSuccess: (c) => {
      avisos.exito(c.activo ? `${c.nombre} puede operar de nuevo` : `${c.nombre} quedó inactivo: sus peticiones se rechazan`)
      clienteConsultas.invalidateQueries({ queryKey: ['canales'] })
    },
    onError: avisos.error,
  })

  if (consulta.isLoading) return <Cargando />
  if (consulta.isError) return <MensajeError error={consulta.error} />
  return (
    <>
      <Stack direction="row" sx={{ justifyContent: 'flex-end', mb: 2 }}>
        <Button variant="contained" color="secondary" onClick={() => setNuevo(true)}>Registrar canal</Button>
      </Stack>
      <Card>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow><TableCell>Código</TableCell><TableCell>Nombre</TableCell><TableCell>Tipo</TableCell><TableCell>Estado</TableCell><TableCell align="right">Acciones</TableCell></TableRow>
            </TableHead>
            <TableBody>
              {consulta.data!.map((c) => (
                <TableRow key={c.codigo}>
                  <TableCell sx={{ fontFamily: 'monospace', fontWeight: 600 }}>{c.codigo}</TableCell>
                  <TableCell>{c.nombre}</TableCell>
                  <TableCell>{nombreTipoCanal[c.tipo]}</TableCell>
                  <TableCell>
                    <Chip size="small" label={c.activo ? 'Activo' : 'Inactivo'}
                          sx={c.activo ? { bgcolor: colores.cafetalClaro, color: colores.cafetalOscuro } : { bgcolor: colores.tejaClara, color: colores.tejaOscura }} />
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                      <Button size="small" loading={renovar.isPending && renovar.variables === c.codigo}
                              onClick={() => renovar.mutate(c.codigo)}>Renovar credencial</Button>
                      <Button size="small" color={c.activo ? 'secondary' : 'primary'}
                              onClick={() => activar.mutate({ codigo: c.codigo, activo: !c.activo })}>
                        {c.activo ? 'Desactivar' : 'Activar'}
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>
      <Alert severity="info" sx={{ mt: 2 }}>
        Cada petición del canal lleva los encabezados <b>X-Canal</b> (código) y <b>X-Credencial-Canal</b>.
        Las operaciones disponibles son <code>GET /api/canal/disponibilidad</code>, <code>POST /api/canal/reservas</code> y{' '}
        <code>DELETE /api/canal/reservas/&#123;idExterno&#125;</code>.
      </Alert>
      {nuevo && <DialogoNuevoCanal alCerrar={() => setNuevo(false)} alRegistrar={alObtenerCredencial} />}
      {credencial && <DialogoCredencial credencial={credencial} alCerrar={() => setCredencial(null)} />}
    </>
  )
}

function DialogoNuevoCanal({ alCerrar, alRegistrar }: { alCerrar: () => void; alRegistrar: (c: CredencialCanal) => void }) {
  const avisos = useAvisos()
  const [codigo, setCodigo] = useState('')
  const [nombre, setNombre] = useState('')
  const [tipo, setTipo] = useState<TipoCanal>('AGENCIA_EN_LINEA')
  const registrar = useMutation({ mutationFn: () => canales.registrar(codigo.trim(), nombre.trim(), tipo), onSuccess: alRegistrar, onError: avisos.error })
  return (
    <Dialog open onClose={alCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>Registrar canal de venta</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ pt: 1 }}>
          <Grid size={{ xs: 12, sm: 5 }}>
            <TextField label="Código" value={codigo} onChange={(e) => setCodigo(e.target.value)}
                       helperText="Identifica al canal en cada petición (ej.: ViajaEje)." />
          </Grid>
          <Grid size={{ xs: 12, sm: 7 }}><TextField label="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} /></Grid>
          <Grid size={12}>
            <TextField select label="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value as TipoCanal)}>
              {TIPOS_CANAL.map((t) => <MenuItem key={t} value={t}>{nombreTipoCanal[t]}</MenuItem>)}
            </TextField>
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button color="inherit" onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" loading={registrar.isPending} disabled={!codigo.trim() || !nombre.trim()}
                onClick={() => registrar.mutate()}>Registrar y generar credencial</Button>
      </DialogActions>
    </Dialog>
  )
}

function DialogoCredencial({ credencial, alCerrar }: { credencial: CredencialCanal; alCerrar: () => void }) {
  const avisos = useAvisos()
  const copiar = async () => {
    await navigator.clipboard.writeText(credencial.credencial)
    avisos.exito('Credencial copiada')
  }
  return (
    <Dialog open onClose={alCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>Credencial de {credencial.canal.nombre}</DialogTitle>
      <DialogContent>
        <Alert severity="warning" sx={{ mb: 2 }}>{credencial.aviso}</Alert>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', p: 1.5, bgcolor: colores.cal, borderRadius: 1 }}>
          <Typography sx={{ fontFamily: 'monospace', overflowWrap: 'anywhere', flexGrow: 1 }}>{credencial.credencial}</Typography>
          <Tooltip title="Copiar"><IconButton onClick={copiar}><CopiarIcon /></IconButton></Tooltip>
        </Stack>
      </DialogContent>
      <DialogActions><Button variant="contained" onClick={alCerrar}>Ya la guardé</Button></DialogActions>
    </Dialog>
  )
}

// --- Conflictos ---

function Conflictos() {
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const [soloPendientes, setSoloPendientes] = useState(true)
  const [pagina, setPagina] = useState(0)
  const [revisar, setRevisar] = useState<ConflictoCanal | null>(null)
  const [nota, setNota] = useState('')
  const consulta = useQuery({ queryKey: ['conflictos', soloPendientes, pagina], queryFn: () => canales.conflictos(soloPendientes, pagina) })
  const guardar = useMutation({
    mutationFn: () => canales.revisarConflicto(revisar!.id, nota.trim()),
    onSuccess: () => {
      avisos.exito('Revisión registrada')
      setRevisar(null)
      clienteConsultas.invalidateQueries({ queryKey: ['conflictos'] })
    },
    onError: avisos.error,
  })

  return (
    <>
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', alignItems: { sm: 'center' }, mb: 2, gap: 1 }}>
        <Typography color="text.secondary">
          Reservas externas rechazadas por chocar con el inventario vendido o bloqueado. Nunca sobrescriben la reserva existente.
        </Typography>
        <FormControlLabel control={<Switch checked={soloPendientes} onChange={(e) => { setSoloPendientes(e.target.checked); setPagina(0) }} />}
                          label="Solo pendientes" sx={{ flexShrink: 0 }} />
      </Stack>
      {consulta.isLoading && <Cargando />}
      {consulta.isError && <MensajeError error={consulta.error} />}
      {consulta.data && consulta.data.contenido.length === 0 && (
        <Vacio texto={soloPendientes ? 'No hay conflictos pendientes de revisión.' : 'No se han registrado conflictos.'} />
      )}
      {consulta.data && consulta.data.contenido.length > 0 && (
        <Card>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Recibido</TableCell><TableCell>Canal · id externo</TableCell><TableCell>Apartamento</TableCell>
                  <TableCell>Estancia</TableCell><TableCell>Titular</TableCell><TableCell>Motivo</TableCell><TableCell>Revisión</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {consulta.data.contenido.map((c) => (
                  <TableRow key={c.id}>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFechaHora(c.registradoEn)}</TableCell>
                    <TableCell sx={{ fontFamily: 'monospace' }}>{c.canal} · {c.idExterno}</TableCell>
                    <TableCell>{c.codigoApartamento}</TableCell>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFecha(c.entrada)} → {formatoFecha(c.salida)}</TableCell>
                    <TableCell>{c.titular}</TableCell>
                    <TableCell sx={{ maxWidth: 260 }}>{c.mensaje}</TableCell>
                    <TableCell sx={{ minWidth: 200 }}>
                      {c.revisado ? (
                        <Box>
                          <Typography variant="body2">{c.notaRevision}</Typography>
                          <Typography variant="caption" color="text.secondary">{c.revisadoPor} · {formatoFechaHora(c.fechaRevision)}</Typography>
                        </Box>
                      ) : (
                        <Button size="small" variant="outlined" onClick={() => { setNota(''); setRevisar(c) }}>Registrar revisión</Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      )}
      {consulta.data && <Paginador pagina={consulta.data.pagina} totalPaginas={consulta.data.totalPaginas} alCambiar={setPagina} />}
      <Dialog open={!!revisar} onClose={() => setRevisar(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Revisar conflicto {revisar?.canal} · {revisar?.idExterno}</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary" sx={{ mb: 2 }}>
            Deje constancia de cómo se resolvió: por ejemplo, si se ofreció otro apartamento al canal o si se avisó al titular.
          </Typography>
          <TextField label="Nota de revisión" multiline minRows={3} value={nota} onChange={(e) => setNota(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button color="inherit" onClick={() => setRevisar(null)}>Cancelar</Button>
          <Button variant="contained" loading={guardar.isPending} disabled={!nota.trim()} onClick={() => guardar.mutate()}>Registrar revisión</Button>
        </DialogActions>
      </Dialog>
    </>
  )
}

// --- Bitácora ---

function Bitacora() {
  const [canal, setCanal] = useState('')
  const [pagina, setPagina] = useState(0)
  const lista = useQuery({ queryKey: ['canales'], queryFn: canales.listar })
  const consulta = useQuery({ queryKey: ['bitacora', canal, pagina], queryFn: () => canales.bitacora(canal, pagina) })

  return (
    <>
      <Stack direction="row" sx={{ mb: 2 }}>
        <TextField select label="Canal" value={canal} sx={{ maxWidth: 280 }}
                   onChange={(e) => { setCanal(e.target.value); setPagina(0) }}>
          <MenuItem value="">Todos los canales</MenuItem>
          {(lista.data ?? []).map((c: CanalVenta) => <MenuItem key={c.codigo} value={c.codigo}>{c.nombre}</MenuItem>)}
        </TextField>
      </Stack>
      {consulta.isLoading && <Cargando />}
      {consulta.isError && <MensajeError error={consulta.error} />}
      {consulta.data && consulta.data.contenido.length === 0 && <Vacio texto="No hay eventos registrados." />}
      {consulta.data && consulta.data.contenido.length > 0 && (
        <Card>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow><TableCell>Fecha</TableCell><TableCell>Canal</TableCell><TableCell>Evento</TableCell><TableCell>Id externo</TableCell><TableCell>Detalle</TableCell></TableRow>
              </TableHead>
              <TableBody>
                {consulta.data.contenido.map((e) => {
                  const estilo = estiloEventoCanal[e.tipo]
                  return (
                    <TableRow key={e.id}>
                      <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFechaHora(e.fecha)}</TableCell>
                      <TableCell sx={{ fontFamily: 'monospace' }}>{e.canal ?? '—'}</TableCell>
                      <TableCell><Chip size="small" label={estilo.texto} sx={{ bgcolor: estilo.fondo, color: estilo.color }} /></TableCell>
                      <TableCell sx={{ fontFamily: 'monospace' }}>{e.idExterno ?? '—'}</TableCell>
                      <TableCell>
                        {e.detalle}
                        {e.idReserva && <> · <Link component={RouterLink} to={`/reservas/${e.idReserva}`}>ver reserva</Link></>}
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      )}
      {consulta.data && <Paginador pagina={consulta.data.pagina} totalPaginas={consulta.data.totalPaginas} alCambiar={setPagina} />}
    </>
  )
}

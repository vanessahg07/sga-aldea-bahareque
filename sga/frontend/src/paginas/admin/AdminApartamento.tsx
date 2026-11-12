import { useState } from 'react'
import { Link as RouterLink, useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Box, Button, Card, CardContent, Chip, Container, Grid, IconButton, Link, List, ListItem, ListItemText,
  MenuItem, Radio, Stack, Tab, Table, TableBody, TableCell, TableHead, TableRow, Tabs, TextField, Tooltip, Typography,
} from '@mui/material'
import BorrarIcon from '@mui/icons-material/DeleteOutlined'
import SubirIcon from '@mui/icons-material/UploadFileOutlined'
import { apartamentos, imagenes as almacen, tarifas } from '../../api/servicios'
import type { ActualizacionApartamento, Apartamento, EstadoOperativo, Imagen } from '../../api/tipos'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, CampoFecha, Confirmacion, EstadoOperativoChip, MensajeError } from '../../componentes/Comunes'
import { DialogoNovedad, HistorialNovedades } from '../../componentes/Novedades'
import { colores } from '../../tema/tema'
import { ESTADOS_OPERATIVOS, estiloOperativo } from '../../utilidades/estados'
import { etiqueta, formatoFecha, formatoFechaHora, formatoPesos, hoyIso, sumarDias } from '../../utilidades/formato'
import { FormularioApartamento, type DatosFormulario } from './FormularioApartamento'

export function AdminApartamento() {
  const { codigo = '' } = useParams()
  const consulta = useQuery({ queryKey: ['apartamento', codigo], queryFn: () => apartamentos.consultar(codigo) })
  if (consulta.isLoading) return <Cargando />
  if (consulta.isError) return <Container sx={{ py: 4 }}><MensajeError error={consulta.error} /></Container>
  return <EditorApartamento key={codigo} apartamento={consulta.data!} />
}

function EditorApartamento({ apartamento: a }: { apartamento: Apartamento }) {
  const codigo = a.codigo
  const navegar = useNavigate()
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const [pestana, setPestana] = useState(0)
  const [datos, setDatos] = useState<DatosFormulario>({ codigo: a.codigo, nombre: a.nombre, descripcion: a.descripcion ?? '',
    dormitorios: a.dormitorios, capacidad: a.capacidad, caracteristicas: a.caracteristicas, admiteMascotas: a.admiteMascotas })
  const [imagenes, setImagenes] = useState<Imagen[]>(a.imagenes)
  const [nuevaImagen, setNuevaImagen] = useState('')
  const [actualizacion, setActualizacion] = useState<ActualizacionApartamento | null>(null)
  const [bloqueo, setBloqueo] = useState({ desde: hoyIso(), hasta: sumarDias(hoyIso(), 2), motivo: '' })
  const [eliminar, setEliminar] = useState(false)
  const [novedad, setNovedad] = useState(false)

  const bloqueos = useQuery({ queryKey: ['bloqueos', codigo], queryFn: () => apartamentos.bloqueos(codigo) })
  const historico = useQuery({ queryKey: ['tarifas', 'historico', codigo], queryFn: () => tarifas.historico(codigo), enabled: pestana === 4 })

  const refrescar = () => {
    clienteConsultas.invalidateQueries({ queryKey: ['apartamento', codigo] })
    clienteConsultas.invalidateQueries({ queryKey: ['apartamentos'] })
    clienteConsultas.invalidateQueries({ queryKey: ['bloqueos', codigo] })
  }
  const mutacion = <T,>(fn: (v: T) => Promise<unknown>, mensaje: string, despues?: (r: unknown) => void) => ({
    mutationFn: fn,
    onSuccess: (r: unknown) => { refrescar(); avisos.exito(mensaje); despues?.(r) },
    onError: avisos.error,
  })

  const guardar = useMutation(mutacion(() => apartamentos.actualizar(codigo, datos), 'Datos actualizados',
    (r) => setActualizacion(r as ActualizacionApartamento)))
  const guardarImagenes = useMutation(mutacion(() => apartamentos.definirImagenes(codigo, imagenes), 'Imágenes guardadas'))
  const activar = useMutation(mutacion(() => apartamentos.activar(codigo), 'El apartamento quedó a la venta'))
  const desactivar = useMutation(mutacion(() => apartamentos.desactivar(codigo), 'El apartamento se retiró de la venta'))
  const borrar = useMutation(mutacion(() => apartamentos.eliminar(codigo), 'Apartamento eliminado', () => navegar('/admin/apartamentos')))
  const bloquear = useMutation(mutacion(() => apartamentos.registrarBloqueo(codigo, bloqueo.desde, bloqueo.hasta, bloqueo.motivo),
    'Bloqueo registrado', () => setBloqueo({ ...bloqueo, motivo: '' })))
  const levantar = useMutation(mutacion((id: string) => apartamentos.levantarBloqueo(id), 'Bloqueo levantado'))
  const cambiarEstado = useMutation(mutacion((e: EstadoOperativo) => apartamentos.cambiarEstado(codigo, e), 'Estado operativo actualizado'))

  const agregarImagen = (url = nuevaImagen.trim()) => {
    if (!url || imagenes.length >= 10) return
    setImagenes((actuales) => [...actuales, { url, principal: actuales.length === 0 }])
    setNuevaImagen('')
  }
  // La imagen se sube al almacén configurado (local o Cloudinary) y se agrega a la galería; se guarda con "Guardar imágenes".
  const subir = useMutation({
    mutationFn: almacen.subir,
    onSuccess: (r) => { agregarImagen(r.url); avisos.exito('Imagen subida. Recuerde guardar las imágenes.') },
    onError: avisos.error,
  })

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Link component={RouterLink} to="/admin/apartamentos" variant="body2">← Inventario</Link>
      <Stack direction={{ xs: 'column', md: 'row' }} sx={{ justifyContent: 'space-between', mt: 1, mb: 2, gap: 2 }}>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
          <Typography variant="h3" component="h1">{a.codigo} · {a.nombre}</Typography>
          {a.activo ? <Chip label="A la venta" color="primary" /> : <Chip label="Inactivo" />}
          <EstadoOperativoChip estado={a.estadoOperativo} />
        </Stack>
        <Stack direction="row" spacing={1}>
          {a.activo
            ? <Button variant="outlined" onClick={() => desactivar.mutate(undefined)} loading={desactivar.isPending}>Retirar de la venta</Button>
            : <Button variant="contained" onClick={() => activar.mutate(undefined)} loading={activar.isPending}>Activar</Button>}
          <Button color="secondary" onClick={() => setEliminar(true)}>Eliminar</Button>
        </Stack>
      </Stack>

      <Tabs value={pestana} onChange={(_, v) => setPestana(v)} sx={{ mb: 3, borderBottom: 1, borderColor: 'divider' }} variant="scrollable">
        <Tab label="Datos" /><Tab label={`Imágenes (${a.imagenes.length})`} /><Tab label="Bloqueos" />
        <Tab label="Estado y novedades" /><Tab label="Histórico de tarifas" />
      </Tabs>

      {pestana === 0 && (
        <Card><CardContent>
          <FormularioApartamento datos={datos} alCambiar={setDatos} />
          <Button variant="contained" sx={{ mt: 2 }} loading={guardar.isPending} onClick={() => guardar.mutate(undefined)}>Guardar datos</Button>
          {actualizacion?.advertencia && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              {actualizacion.advertencia}
              <Box component="ul" sx={{ m: 0, pl: 2 }}>
                {actualizacion.reservasSobreCapacidad.map((r) => (
                  <li key={r.id}>
                    <Link component={RouterLink} to={`/reservas/${r.id}`}>{r.titular}</Link> · {formatoFecha(r.entrada)} · {r.totalOcupantes} personas
                  </li>
                ))}
              </Box>
            </Alert>
          )}
        </CardContent></Card>
      )}

      {pestana === 1 && (
        <Card><CardContent>
          <Typography color="text.secondary" sx={{ mb: 2 }}>Entre 1 y 10 imágenes. La principal se muestra en los resultados de búsqueda.</Typography>
          <Grid container spacing={2}>
            {imagenes.map((img, i) => (
              <Grid key={img.url + i} size={{ xs: 6, sm: 4, md: 3 }}>
                <Box sx={{ position: 'relative', border: 2, borderColor: img.principal ? colores.teja : 'transparent', borderRadius: 2, overflow: 'hidden' }}>
                  <Box component="img" src={img.url} alt="" sx={{ width: '100%', height: 120, objectFit: 'cover', display: 'block', bgcolor: colores.borde }} />
                  <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', px: 0.5 }}>
                    <Tooltip title="Imagen principal">
                      <Radio size="small" checked={img.principal} onChange={() => setImagenes(imagenes.map((x, j) => ({ ...x, principal: j === i })))} />
                    </Tooltip>
                    <IconButton size="small" aria-label="Quitar imagen" onClick={() => {
                      const resto = imagenes.filter((_, j) => j !== i)
                      if (img.principal && resto.length) resto[0] = { ...resto[0], principal: true }
                      setImagenes(resto)
                    }}><BorrarIcon fontSize="small" /></IconButton>
                  </Stack>
                </Box>
              </Grid>
            ))}
          </Grid>
          <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
            <TextField label="URL de la imagen" value={nuevaImagen} onChange={(e) => setNuevaImagen(e.target.value)} disabled={imagenes.length >= 10}
                       onKeyDown={(e) => { if (e.key === 'Enter') agregarImagen() }} />
            <Button onClick={() => agregarImagen()} disabled={imagenes.length >= 10}>Agregar</Button>
            <Button component="label" variant="outlined" startIcon={<SubirIcon />} loading={subir.isPending}
                    disabled={imagenes.length >= 10} sx={{ flexShrink: 0 }}>
              Subir archivo
              <input hidden type="file" accept="image/jpeg,image/png,image/webp"
                     onChange={(e) => { const archivo = e.target.files?.[0]; if (archivo) subir.mutate(archivo); e.target.value = '' }} />
            </Button>
          </Stack>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
            JPG, PNG o WEBP de máximo 5 MB. Si el servicio de imágenes no responde, puede pegar la URL de una imagen ya publicada.
          </Typography>
          <Button variant="contained" sx={{ mt: 2 }} loading={guardarImagenes.isPending} disabled={imagenes.length === 0}
                  onClick={() => guardarImagenes.mutate(undefined)}>Guardar imágenes</Button>
        </CardContent></Card>
      )}

      {pestana === 2 && (
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 5 }}>
            <Card><CardContent>
              <Typography variant="h5" sx={{ mb: 2 }}>Nuevo bloqueo</Typography>
              <Stack spacing={2}>
                <Stack direction="row" spacing={1.5}>
                  <CampoFecha label="Desde" value={bloqueo.desde} onChange={(e) => setBloqueo({ ...bloqueo, desde: e.target.value })} />
                  <CampoFecha label="Hasta (sin incluir)" value={bloqueo.hasta} onChange={(e) => setBloqueo({ ...bloqueo, hasta: e.target.value })} />
                </Stack>
                <TextField label="Motivo" value={bloqueo.motivo} onChange={(e) => setBloqueo({ ...bloqueo, motivo: e.target.value })} />
                <Button variant="contained" loading={bloquear.isPending} disabled={!bloqueo.motivo.trim()} onClick={() => bloquear.mutate(undefined)}>
                  Registrar bloqueo
                </Button>
                <Typography variant="caption" color="text.secondary">No se pueden bloquear noches que ya tienen reservas activas.</Typography>
              </Stack>
            </CardContent></Card>
          </Grid>
          <Grid size={{ xs: 12, md: 7 }}>
            <Card><CardContent>
              <Typography variant="h5" sx={{ mb: 1 }}>Bloqueos vigentes</Typography>
              {bloqueos.data?.length === 0 && <Typography color="text.secondary">No hay bloqueos vigentes.</Typography>}
              <List disablePadding>
                {bloqueos.data?.map((b) => (
                  <ListItem key={b.id} divider disableGutters secondaryAction={
                    <Button size="small" color="secondary" onClick={() => levantar.mutate(b.id)}>Levantar</Button>}>
                    <ListItemText primary={b.motivo} secondary={`${formatoFecha(b.desde)} → ${formatoFecha(b.hasta)}`} />
                  </ListItem>
                ))}
              </List>
            </CardContent></Card>
          </Grid>
        </Grid>
      )}

      {pestana === 3 && (
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 5 }}>
            <Card><CardContent>
              <Typography variant="h5" sx={{ mb: 2 }}>Estado operativo</Typography>
              <TextField select label="Cambiar a" value="" onChange={(e) => cambiarEstado.mutate(e.target.value as EstadoOperativo)}>
                {ESTADOS_OPERATIVOS.filter((e) => e !== a.estadoOperativo && e !== 'OCUPADO').map((e) => (
                  <MenuItem key={e} value={e}>{estiloOperativo[e].texto}</MenuItem>
                ))}
              </TextField>
              <Alert severity="info" sx={{ mt: 2 }}>
                Declarar fuera de servicio no impide vender las noches futuras: registre también un bloqueo.
              </Alert>
            </CardContent></Card>
          </Grid>
          <Grid size={{ xs: 12, md: 7 }}>
            <Card><CardContent>
              <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="h5">Novedades</Typography>
                <Button size="small" onClick={() => setNovedad(true)}>Registrar novedad</Button>
              </Stack>
              <HistorialNovedades codigo={codigo} />
            </CardContent></Card>
          </Grid>
        </Grid>
      )}

      {pestana === 4 && (
        <Card>
          {historico.isLoading && <Cargando />}
          {historico.data && (
            <Table size="small">
              <TableHead><TableRow><TableCell>Temporada</TableCell><TableCell align="right">Valor</TableCell><TableCell>Vigente desde</TableCell></TableRow></TableHead>
              <TableBody>
                {historico.data.map((t, i) => (
                  <TableRow key={i}>
                    <TableCell>{etiqueta(t.temporada)}</TableCell>
                    <TableCell align="right">{formatoPesos(t.valor)}</TableCell>
                    <TableCell>{formatoFechaHora(t.vigenteDesde)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
          <CardContent>
            <Link component={RouterLink} to="/admin/tarifas">Editar tarifas</Link>
          </CardContent>
        </Card>
      )}

      <Confirmacion abierto={eliminar} titulo={`Eliminar ${a.codigo}`} peligrosa textoConfirmar="Eliminar" cargando={borrar.isPending}
                    alCerrar={() => setEliminar(false)} alConfirmar={() => borrar.mutate(undefined)}>
        El apartamento deja de aparecer en listados y búsquedas; sus reservas históricas se conservan.
        Solo se puede eliminar si no tiene reservas activas ni futuras.
      </Confirmacion>
      <DialogoNovedad codigo={novedad ? codigo : null} alCerrar={() => setNovedad(false)} />
    </Container>
  )
}

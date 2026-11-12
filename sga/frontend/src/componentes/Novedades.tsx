import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, List, ListItem, ListItemText, MenuItem, Stack,
  TextField, Typography,
} from '@mui/material'
import IaIcon from '@mui/icons-material/AutoAwesomeOutlined'
import { apartamentos, ia } from '../api/servicios'
import type { Gravedad, SugerenciaGravedad } from '../api/tipos'
import { useAvisos } from './Avisos'
import { Cargando, GravedadChip, MensajeError, Paginador } from './Comunes'
import { estiloGravedad, GRAVEDADES } from '../utilidades/estados'
import { formatoFechaHora } from '../utilidades/formato'

/** Registro de una novedad (daño, faltante o situación) sobre un apartamento. */
export function DialogoNovedad({ codigo, alCerrar }: { codigo: string | null; alCerrar: () => void }) {
  return codigo ? <FormularioNovedad codigo={codigo} alCerrar={alCerrar} /> : null
}

function FormularioNovedad({ codigo, alCerrar }: { codigo: string; alCerrar: () => void }) {
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const [descripcion, setDescripcion] = useState('')
  const [gravedad, setGravedad] = useState<Gravedad>('BAJA')
  const [sugerencia, setSugerencia] = useState<SugerenciaGravedad | null>(null)
  const estadoIa = useQuery({ queryKey: ['ia'], queryFn: ia.estado, staleTime: Infinity })
  const iaHabilitada = estadoIa.data?.habilitada ?? false

  // La IA solo sugiere: nunca bloquea el registro y la persona decide la gravedad final.
  const sugerir = useMutation({
    mutationFn: () => ia.sugerirGravedad(descripcion),
    onSuccess: (s) => {
      setSugerencia(s)
      if (s.disponible && s.gravedad) setGravedad(s.gravedad)
    },
    onError: () => setSugerencia({ disponible: false, gravedad: null, justificacion: null, proveedor: null,
      mensaje: 'La sugerencia de IA no está disponible en este momento. Elija la gravedad manualmente.' }),
  })

  const registrar = useMutation({
    mutationFn: () => apartamentos.registrarNovedad(codigo, descripcion, gravedad),
    onSuccess: () => {
      avisos.exito('Novedad registrada')
      clienteConsultas.invalidateQueries({ queryKey: ['novedades', codigo] })
      alCerrar()
    },
    onError: avisos.error,
  })

  return (
    <Dialog open onClose={alCerrar} maxWidth="xs" fullWidth>
      <DialogTitle>Registrar novedad en {codigo}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField label="Descripción" multiline minRows={3} value={descripcion}
                     onChange={(e) => { setDescripcion(e.target.value); setSugerencia(null) }}
                     placeholder="Ej.: la ducha del baño principal gotea" />
          <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
            <TextField select label="Gravedad" value={gravedad} onChange={(e) => setGravedad(e.target.value as Gravedad)}>
              {GRAVEDADES.map((g) => <MenuItem key={g} value={g}>{estiloGravedad[g].texto}</MenuItem>)}
            </TextField>
            <Button startIcon={<IaIcon />} loading={sugerir.isPending} disabled={!descripcion.trim() || !iaHabilitada}
                    onClick={() => sugerir.mutate()} sx={{ flexShrink: 0, mt: 1 }}>
              Sugerir
            </Button>
          </Stack>
          {estadoIa.data && !iaHabilitada && (
            <Typography variant="caption" color="text.secondary">
              La sugerencia con IA está deshabilitada. Elija la gravedad según su criterio.
            </Typography>
          )}
          {sugerencia && (sugerencia.disponible
            ? <Alert severity="info" icon={<IaIcon fontSize="inherit" />}>
                Sugerencia: <b>{estiloGravedad[sugerencia.gravedad!].texto}</b>. {sugerencia.justificacion} Puede cambiarla antes de registrar.
              </Alert>
            : <Alert severity="warning">{sugerencia.mensaje}</Alert>)}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button color="inherit" onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" loading={registrar.isPending} disabled={!descripcion.trim()} onClick={() => registrar.mutate()}>
          Registrar
        </Button>
      </DialogActions>
    </Dialog>
  )
}

/** Historial de novedades de un apartamento, de la más reciente a la más antigua. */
export function HistorialNovedades({ codigo }: { codigo: string }) {
  const [pagina, setPagina] = useState(0)
  const consulta = useQuery({ queryKey: ['novedades', codigo, pagina], queryFn: () => apartamentos.novedades(codigo, pagina) })
  if (consulta.isLoading) return <Cargando />
  if (consulta.isError) return <MensajeError error={consulta.error} />
  const datos = consulta.data!
  if (datos.contenido.length === 0) return <Typography color="text.secondary">No hay novedades registradas.</Typography>
  return (
    <>
      <List disablePadding>
        {datos.contenido.map((n) => (
          <ListItem key={n.id} divider disableGutters>
            <ListItemText
              primary={<Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}><GravedadChip gravedad={n.gravedad} /><span>{n.descripcion}</span></Stack>}
              secondary={`${formatoFechaHora(n.fecha)} · ${n.autor}`} />
          </ListItem>
        ))}
      </List>
      <Paginador pagina={datos.pagina} totalPaginas={datos.totalPaginas} alCambiar={setPagina} />
    </>
  )
}

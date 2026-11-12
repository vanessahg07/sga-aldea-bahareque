import type { ReactNode } from 'react'
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, Pagination,
  Stack, TextField, Typography, type TextFieldProps,
} from '@mui/material'
import { mensajeDe } from '../api/cliente'
import type { EstadoOperativo, EstadoReserva, Gravedad } from '../api/tipos'
import { estiloGravedad, estiloOperativo, estiloReserva } from '../utilidades/estados'

export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', py: 4, justifyContent: 'center' }}>
      <CircularProgress size={22} />
      <Typography color="text.secondary">{texto}</Typography>
    </Stack>
  )
}

export function MensajeError({ error, reintentar }: { error: unknown; reintentar?: () => void }) {
  return (
    <Alert severity="error" action={reintentar ? <Button color="inherit" onClick={reintentar}>Reintentar</Button> : undefined}>
      {mensajeDe(error)}
    </Alert>
  )
}

function ChipEstado({ texto, fondo, color, pequeno }: { texto: string; fondo: string; color: string; pequeno?: boolean }) {
  return <Chip label={texto} size={pequeno ? 'small' : 'medium'} sx={{ bgcolor: fondo, color }} />
}

export function EstadoReservaChip({ estado, pequeno }: { estado: EstadoReserva; pequeno?: boolean }) {
  return <ChipEstado {...estiloReserva[estado]} pequeno={pequeno} />
}

export function EstadoOperativoChip({ estado, pequeno }: { estado: EstadoOperativo; pequeno?: boolean }) {
  return <ChipEstado {...estiloOperativo[estado]} pequeno={pequeno} />
}

export function GravedadChip({ gravedad }: { gravedad: Gravedad }) {
  return <ChipEstado {...estiloGravedad[gravedad]} pequeno />
}

/** Paginador de la API (páginas desde 0) con la numeración visible desde 1. */
export function Paginador({ pagina, totalPaginas, alCambiar }: {
  pagina: number
  totalPaginas: number
  alCambiar: (pagina: number) => void
}) {
  if (totalPaginas <= 1) return null
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
      <Pagination count={totalPaginas} page={pagina + 1} onChange={(_, p) => alCambiar(p - 1)} color="primary" shape="rounded" />
    </Box>
  )
}

/** Campo de fecha nativo con la etiqueta siempre visible. */
export function CampoFecha(props: TextFieldProps) {
  return <TextField type="date" {...props} slotProps={{ inputLabel: { shrink: true }, ...props.slotProps }} />
}

export function CampoHora(props: TextFieldProps) {
  return <TextField type="time" {...props} slotProps={{ inputLabel: { shrink: true }, ...props.slotProps }} />
}

export function Encabezado({ titulo, subtitulo, acciones }: { titulo: string; subtitulo?: ReactNode; acciones?: ReactNode }) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}
           sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'center' }, mb: 3 }}>
      <Box>
        <Typography variant="h3" component="h1">{titulo}</Typography>
        {subtitulo && <Typography color="text.secondary" sx={{ mt: 0.5 }}>{subtitulo}</Typography>}
      </Box>
      {acciones && <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>{acciones}</Stack>}
    </Stack>
  )
}

/** Diálogo de confirmación para acciones que cambian el estado de un registro. */
export function Confirmacion({ abierto, titulo, children, textoConfirmar = 'Confirmar', peligrosa, cargando, alConfirmar, alCerrar }: {
  abierto: boolean
  titulo: string
  children: ReactNode
  textoConfirmar?: string
  peligrosa?: boolean
  cargando?: boolean
  alConfirmar: () => void
  alCerrar: () => void
}) {
  return (
    <Dialog open={abierto} onClose={alCerrar} maxWidth="xs" fullWidth>
      <DialogTitle>{titulo}</DialogTitle>
      <DialogContent>{children}</DialogContent>
      <DialogActions>
        <Button onClick={alCerrar} color="inherit">Volver</Button>
        <Button onClick={alConfirmar} variant="contained" color={peligrosa ? 'secondary' : 'primary'} loading={cargando}>
          {textoConfirmar}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export function Vacio({ texto }: { texto: string }) {
  return (
    <Box sx={{ py: 5, textAlign: 'center', color: 'text.secondary', border: '1px dashed', borderColor: 'divider', borderRadius: 2 }}>
      <Typography>{texto}</Typography>
    </Box>
  )
}

export function Dato({ etiqueta, children }: { etiqueta: string; children: ReactNode }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>{etiqueta}</Typography>
      <Typography component="div" sx={{ overflowWrap: 'anywhere' }}>{children}</Typography>
    </Box>
  )
}

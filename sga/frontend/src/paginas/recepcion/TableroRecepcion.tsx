import { useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Box, Button, Card, CardContent, CardHeader, Container, Grid, List, ListItem, ListItemButton, ListItemText, Stack,
  Typography,
} from '@mui/material'
import LlegadaIcon from '@mui/icons-material/LoginRounded'
import SalidaIcon from '@mui/icons-material/LogoutRounded'
import LimpiezaIcon from '@mui/icons-material/CleaningServicesOutlined'
import { apartamentos, reservas } from '../../api/servicios'
import type { ReservaResumen } from '../../api/tipos'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, CampoFecha, Encabezado, EstadoReservaChip, MensajeError } from '../../componentes/Comunes'
import { colores } from '../../tema/tema'
import { formatoDia, formatoHora, formatoPesos, hoyIso } from '../../utilidades/formato'

function Indicador({ icono, valor, texto, color }: { icono: React.ReactNode; valor: number | string; texto: string; color: string }) {
  return (
    <Card>
      <CardContent>
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          <Box sx={{ width: 48, height: 48, borderRadius: 2, display: 'grid', placeItems: 'center', bgcolor: color }}>{icono}</Box>
          <Box>
            <Typography variant="h3">{valor}</Typography>
            <Typography color="text.secondary">{texto}</Typography>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

export function TableroRecepcion() {
  const navegar = useNavigate()
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const [fecha, setFecha] = useState(hoyIso())

  const llegadas = useQuery({ queryKey: ['reservas', 'llegadas', fecha], queryFn: () => reservas.llegadas(fecha) })
  const salidas = useQuery({ queryKey: ['reservas', 'salidas', fecha], queryFn: () => reservas.salidas(fecha) })
  const pendientes = useQuery({ queryKey: ['apartamentos', 'pendientes'], queryFn: apartamentos.pendientesPreparacion })

  const registrarLlegada = useMutation({
    mutationFn: (id: string) => reservas.registrarLlegada(id),
    onSuccess: (r) => {
      avisos.exito(`Llegada registrada en ${r.codigoApartamento}`)
      clienteConsultas.invalidateQueries({ queryKey: ['reservas'] })
    },
    onError: avisos.error,
  })

  const fila = (r: ReservaResumen, accion: React.ReactNode) => (
    <ListItem key={r.id} divider secondaryAction={accion} disablePadding>
      <ListItemButton onClick={() => navegar(`/reservas/${r.id}`)} sx={{ pr: 22 }}>
        <ListItemText
          primary={<Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Typography sx={{ fontWeight: 600 }}>{r.codigoApartamento}</Typography>
            <Typography>{r.titular}</Typography>
            <EstadoReservaChip estado={r.estado} pequeno />
          </Stack>}
          secondary={`${r.totalOcupantes} personas · llegada estimada ${formatoHora(r.horaEstimadaLlegada)} · saldo ${formatoPesos(r.saldo)}`} />
      </ListItemButton>
    </ListItem>
  )

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Encabezado titulo="Recepción" subtitulo={`Movimientos del ${formatoDia(fecha)}`}
                  acciones={<>
                    <CampoFecha label="Día" value={fecha} onChange={(e) => setFecha(e.target.value)} sx={{ width: 170 }} />
                    <Button variant="contained" color="secondary" component={RouterLink} to="/recepcion/nueva">Reserva directa</Button>
                  </>} />
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Indicador icono={<LlegadaIcon sx={{ color: colores.cafetalOscuro }} />} valor={llegadas.data?.length ?? '—'} texto="Llegadas previstas" color={colores.cafetalClaro} />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Indicador icono={<SalidaIcon sx={{ color: colores.nieblaOscura }} />} valor={salidas.data?.length ?? '—'} texto="Salidas previstas" color={colores.niebla} />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Indicador icono={<LimpiezaIcon sx={{ color: colores.guaduaOscura }} />} valor={pendientes.data?.length ?? '—'} texto="Por preparar" color={colores.guaduaClara} />
        </Grid>
      </Grid>
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardHeader title="Llegadas" slotProps={{ title: { variant: 'h5' } }} />
            {llegadas.isLoading && <Cargando />}
            {llegadas.isError && <MensajeError error={llegadas.error} />}
            <List disablePadding>
              {llegadas.data?.map((r) => fila(r, r.estado === 'CONFIRMADA' && fecha === hoyIso()
                ? <Button size="small" variant="contained" loading={registrarLlegada.isPending && registrarLlegada.variables === r.id}
                          onClick={() => registrarLlegada.mutate(r.id)}>Registrar llegada</Button>
                : <Button size="small" onClick={() => navegar(`/reservas/${r.id}`)}>Ver</Button>))}
              {llegadas.data?.length === 0 && <ListItem><ListItemText secondary="No hay llegadas previstas." /></ListItem>}
            </List>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardHeader title="Salidas" slotProps={{ title: { variant: 'h5' } }} />
            {salidas.isLoading && <Cargando />}
            {salidas.isError && <MensajeError error={salidas.error} />}
            <List disablePadding>
              {salidas.data?.map((r) => fila(r, <Button size="small" variant="outlined" onClick={() => navegar(`/reservas/${r.id}`)}>
                {r.saldo === 0 ? 'Registrar salida' : 'Cobrar y dar salida'}
              </Button>))}
              {salidas.data?.length === 0 && <ListItem><ListItemText secondary="No hay salidas previstas." /></ListItem>}
            </List>
          </Card>
        </Grid>
      </Grid>
    </Container>
  )
}

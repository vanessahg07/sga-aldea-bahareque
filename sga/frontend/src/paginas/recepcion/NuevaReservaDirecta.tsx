import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  Alert, Button, Card, CardContent, Container, Divider, Grid, MenuItem, Stack, TextField, Typography,
} from '@mui/material'
import { apartamentos, disponibilidad, reservas } from '../../api/servicios'
import type { Ocupante, ResultadoCotizacion } from '../../api/tipos'
import { useAvisos } from '../../componentes/Avisos'
import { CampoFecha, CampoHora, Encabezado } from '../../componentes/Comunes'
import { DesgloseCotizacion, EditorOcupantes } from '../../componentes/Negocio'
import { hoyIso, sumarDias } from '../../utilidades/formato'

/** Reserva por el canal directo: la recepción registra un titular que puede no tener cuenta. */
export function NuevaReservaDirecta() {
  const [parametros] = useSearchParams()
  const navegar = useNavigate()
  const avisos = useAvisos()
  const inventario = useQuery({ queryKey: ['apartamentos', 'venta'], queryFn: () => apartamentos.aLaVenta(0) })

  const [codigo, setCodigo] = useState(parametros.get('apartamento') ?? '')
  const [entrada, setEntrada] = useState(parametros.get('entrada') ?? hoyIso())
  const [salida, setSalida] = useState(parametros.get('salida') ?? sumarDias(hoyIso(), 2))
  const [hora, setHora] = useState('15:00')
  const [mascotas, setMascotas] = useState(0)
  const [titular, setTitular] = useState({ nombre: '', documento: '', correo: '', telefono: '', fechaNacimiento: '' })
  const [acompanantes, setAcompanantes] = useState<Ocupante[]>([])
  const [cotizacion, setCotizacion] = useState<ResultadoCotizacion | null>(null)

  const apartamento = inventario.data?.contenido.find((a) => a.codigo === codigo)
  const titularCompleto = titular.nombre.trim() && titular.documento.trim() && titular.fechaNacimiento
  const acompanantesCompletos = acompanantes.every((o) => o.nombre.trim() && o.fechaNacimiento)
  const invalidar = () => setCotizacion(null)

  const cotizar = useMutation({
    mutationFn: () => disponibilidad.cotizar(codigo, entrada, salida,
      [titular.fechaNacimiento, ...acompanantes.map((a) => a.fechaNacimiento)], mascotas),
    onSuccess: setCotizacion,
    onError: avisos.error,
  })
  const crear = useMutation({
    mutationFn: () => reservas.crearDirecta(titular, {
      codigoApartamento: codigo, entrada, salida, acompanantes, horaEstimadaLlegada: hora || null, mascotas,
    }),
    onSuccess: (r) => { avisos.exito('Reserva directa creada'); navegar(`/reservas/${r.id}`) },
    onError: avisos.error,
  })

  const campoTitular = (campo: keyof typeof titular) => ({
    value: titular[campo],
    onChange: (e: { target: { value: string } }) => { setTitular({ ...titular, [campo]: e.target.value }); invalidar() },
  })

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Encabezado titulo="Reserva directa" subtitulo="Reservas recibidas por teléfono, mensajería o en persona." />
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 7 }}>
          <Stack spacing={3}>
            <Card>
              <CardContent>
                <Typography variant="h5" sx={{ mb: 2 }}>Estancia</Typography>
                <Grid container spacing={2}>
                  <Grid size={12}>
                    <TextField select label="Apartamento" value={codigo} onChange={(e) => { setCodigo(e.target.value); invalidar() }}>
                      {inventario.data
                        ? inventario.data.contenido.map((a) => (
                          <MenuItem key={a.codigo} value={a.codigo}>{a.codigo} · {a.nombre} · hasta {a.capacidad} personas</MenuItem>
                        ))
                        : <MenuItem value={codigo}>{codigo || 'Cargando…'}</MenuItem>}
                    </TextField>
                  </Grid>
                  <Grid size={{ xs: 6, sm: 3 }}><CampoFecha label="Llegada" value={entrada} onChange={(e) => { setEntrada(e.target.value); invalidar() }} /></Grid>
                  <Grid size={{ xs: 6, sm: 3 }}><CampoFecha label="Salida" value={salida} onChange={(e) => { setSalida(e.target.value); invalidar() }} /></Grid>
                  <Grid size={{ xs: 6, sm: 3 }}><CampoHora label="Llegada estimada" value={hora} onChange={(e) => setHora(e.target.value)} /></Grid>
                  <Grid size={{ xs: 6, sm: 3 }}>
                    <TextField select label="Mascotas" value={mascotas} disabled={!apartamento?.admiteMascotas}
                               onChange={(e) => { setMascotas(Number(e.target.value)); invalidar() }}>
                      {[0, 1, 2].map((n) => <MenuItem key={n} value={n}>{n}</MenuItem>)}
                    </TextField>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
            <Card>
              <CardContent>
                <Typography variant="h5" sx={{ mb: 2 }}>Titular</Typography>
                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 6 }}><TextField label="Nombre completo" required {...campoTitular('nombre')} /></Grid>
                  <Grid size={{ xs: 12, sm: 6 }}><TextField label="Documento" required {...campoTitular('documento')} /></Grid>
                  <Grid size={{ xs: 12, sm: 6 }}><TextField label="Correo (para notificaciones)" type="email" {...campoTitular('correo')} /></Grid>
                  <Grid size={{ xs: 12, sm: 6 }}><TextField label="Teléfono" {...campoTitular('telefono')} /></Grid>
                  <Grid size={{ xs: 12, sm: 6 }}><CampoFecha label="Fecha de nacimiento" required {...campoTitular('fechaNacimiento')} /></Grid>
                </Grid>
                <Divider sx={{ my: 2 }} />
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Acompañantes</Typography>
                <EditorOcupantes ocupantes={acompanantes} alCambiar={(o) => { setAcompanantes(o); invalidar() }}
                                 maximo={apartamento ? apartamento.capacidad - 1 : undefined} />
              </CardContent>
            </Card>
          </Stack>
        </Grid>
        <Grid size={{ xs: 12, md: 5 }}>
          <Card sx={{ position: { md: 'sticky' }, top: { md: 88 } }}>
            <CardContent>
              <Typography variant="h5" sx={{ mb: 2 }}>Cotización</Typography>
              <Stack spacing={2}>
                <Button variant="outlined" onClick={() => cotizar.mutate()} loading={cotizar.isPending}
                        disabled={!codigo || !titularCompleto || !acompanantesCompletos}>
                  Cotizar
                </Button>
                {cotizacion && !cotizacion.disponible && <Alert severity="warning">{cotizacion.mensajeNoDisponible}</Alert>}
                {cotizacion && <DesgloseCotizacion cotizacion={cotizacion.cotizacion} compacto />}
                <Button variant="contained" color="secondary" size="large" loading={crear.isPending}
                        disabled={!cotizacion?.disponible} onClick={() => crear.mutate()}>
                  Crear reserva
                </Button>
                <Typography variant="caption" color="text.secondary">
                  La reserva queda pendiente hasta registrar el anticipo y confirmarla.
                </Typography>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Container>
  )
}

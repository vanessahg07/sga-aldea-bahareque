import { useState } from 'react'
import { Link as RouterLink, useNavigate, useParams, useSearchParams } from 'react-router'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  Alert, Box, Button, Card, CardContent, Chip, Container, Divider, Grid, MenuItem, Stack, Table, TableBody,
  TableCell, TableRow, TextField, Typography,
} from '@mui/material'
import PersonasIcon from '@mui/icons-material/PeopleAltOutlined'
import CamaIcon from '@mui/icons-material/BedOutlined'
import MascotaIcon from '@mui/icons-material/PetsOutlined'
import { alojamiento, apartamentos, disponibilidad, reservas } from '../../api/servicios'
import type { Ocupante, ResultadoCotizacion } from '../../api/tipos'
import { useSesion } from '../../sesion/Sesion'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, CampoFecha, CampoHora, MensajeError } from '../../componentes/Comunes'
import { CalendarioDisponibilidad, DesgloseCotizacion, EditorOcupantes, MapaUbicacion } from '../../componentes/Negocio'
import { colores } from '../../tema/tema'
import { etiqueta, fechasDelGrupo, formatoHora, formatoPesos, hoyIso, sumarDias } from '../../utilidades/formato'

export function DetalleApartamento() {
  const { codigo = '' } = useParams()
  const [parametros] = useSearchParams()
  const navegar = useNavigate()
  const avisos = useAvisos()
  const { usuario, tieneRol } = useSesion()

  const apartamento = useQuery({ queryKey: ['apartamento', codigo], queryFn: () => apartamentos.consultar(codigo) })
  const datosAlojamiento = useQuery({ queryKey: ['alojamiento'], queryFn: alojamiento.consultar })
  const tarifas = useQuery({ queryKey: ['tarifas', codigo], queryFn: () => apartamentos.tarifas(codigo) })
  const desdeCalendario = hoyIso()
  const calendario = useQuery({
    queryKey: ['calendario', codigo, desdeCalendario],
    queryFn: () => apartamentos.calendario(codigo, desdeCalendario, sumarDias(desdeCalendario, 61)),
  })

  const [imagen, setImagen] = useState(0)
  const [entrada, setEntrada] = useState(parametros.get('entrada') ?? sumarDias(hoyIso(), 14))
  const [salida, setSalida] = useState(parametros.get('salida') ?? sumarDias(hoyIso(), 16))
  const [hora, setHora] = useState('16:00')
  const [mascotas, setMascotas] = useState(Number(parametros.get('mascotas') ?? 0))
  const [acompanantes, setAcompanantes] = useState<Ocupante[]>([])
  const [cotizacion, setCotizacion] = useState<ResultadoCotizacion | null>(null)

  const adultosBusqueda = Number(parametros.get('adultos') ?? 1)
  const menoresBusqueda = Number(parametros.get('menores') ?? 0)
  const titularNacimiento = usuario?.fechaNacimiento
  const fechasGrupo = () => acompanantes.length > 0 || usuario
    ? [titularNacimiento ?? fechasDelGrupo(1, 0, entrada)[0], ...acompanantes.map((a) => a.fechaNacimiento)]
    : fechasDelGrupo(adultosBusqueda, menoresBusqueda, entrada)

  const cotizar = useMutation({
    mutationFn: () => disponibilidad.cotizar(codigo, entrada, salida, fechasGrupo(), mascotas),
    onSuccess: setCotizacion,
    onError: avisos.error,
  })

  const reservar = useMutation({
    mutationFn: () => reservas.crearPorPortal({
      codigoApartamento: codigo, entrada, salida, acompanantes, horaEstimadaLlegada: hora, mascotas,
    }),
    onSuccess: (reserva) => {
      avisos.exito('Reserva creada. Queda pendiente hasta que se registre el anticipo.')
      navegar(`/reservas/${reserva.id}`)
    },
    onError: avisos.error,
  })

  if (apartamento.isLoading) return <Cargando />
  if (apartamento.isError) return <Container sx={{ py: 4 }}><MensajeError error={apartamento.error} /></Container>
  const a = apartamento.data!
  const aloj = datosAlojamiento.data
  const imagenes = a.imagenes.length ? a.imagenes : [{ url: '', principal: true }]
  const acompanantesIncompletos = acompanantes.some((o) => !o.nombre.trim() || !o.fechaNacimiento)

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="overline" color="secondary">{a.codigo}</Typography>
      <Typography variant="h2" component="h1">{a.nombre}</Typography>
      <Stack direction="row" spacing={3} sx={{ mt: 1, mb: 3, color: 'text.secondary', flexWrap: 'wrap' }}>
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}><PersonasIcon fontSize="small" /><span>Hasta {a.capacidad} personas</span></Stack>
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}><CamaIcon fontSize="small" /><span>{a.dormitorios} {a.dormitorios === 1 ? 'dormitorio' : 'dormitorios'}</span></Stack>
        {a.admiteMascotas && <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}><MascotaIcon fontSize="small" /><span>Admite mascotas</span></Stack>}
      </Stack>

      <Grid container spacing={1.5} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, md: 8 }}>
          <Box component="img" src={imagenes[imagen].url} alt={`${a.nombre}, imagen ${imagen + 1}`}
               sx={{ width: '100%', height: { xs: 240, md: 420 }, objectFit: 'cover', borderRadius: 2, bgcolor: colores.borde }} />
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(4, 1fr)', md: '1fr 1fr' }, gap: 1.5 }}>
            {imagenes.map((img, i) => (
              <Box key={img.url + i} component="img" src={img.url} alt={`Miniatura ${i + 1}`} onClick={() => setImagen(i)}
                   sx={{ width: '100%', height: { xs: 70, md: 130 }, objectFit: 'cover', borderRadius: 1.5, cursor: 'pointer',
                         outline: i === imagen ? `3px solid ${colores.teja}` : 'none', bgcolor: colores.borde }} />
            ))}
          </Box>
        </Grid>
      </Grid>

      <Grid container spacing={4}>
        <Grid size={{ xs: 12, md: 7 }}>
          <Stack spacing={4}>
            <Box>
              <Typography sx={{ fontSize: '1.1rem' }}>{a.descripcion}</Typography>
              <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1, mt: 2 }}>
                {a.caracteristicas.map((c) => <Chip key={c} label={etiqueta(c)} />)}
              </Stack>
            </Box>

            <Box>
              <Typography variant="h4" sx={{ mb: 1.5 }}>Tarifas por persona y noche</Typography>
              <Card>
                <Table size="small">
                  <TableBody>
                    {tarifas.data?.map((t) => (
                      <TableRow key={t.temporada}>
                        <TableCell>{etiqueta(t.temporada)}</TableCell>
                        <TableCell align="right">{formatoPesos(t.valor)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Card>
              {aloj && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  Los menores de {aloj.parametros.umbralEdadFacturable} años no pagan. Estancias de {aloj.parametros.nochesDescuentoLargo} noches
                  o más tienen {aloj.parametros.porcentajeDescuentoLargo}% de descuento.
                  {a.admiteMascotas && ` Mascotas: ${formatoPesos(aloj.parametros.cargoMascota)} por estancia.`}
                </Typography>
              )}
            </Box>

            <Box>
              <Typography variant="h4" sx={{ mb: 1.5 }}>Disponibilidad</Typography>
              {calendario.data ? <CalendarioDisponibilidad noches={calendario.data} /> : <Cargando />}
            </Box>

            {aloj && (
              <Box>
                <Typography variant="h4" sx={{ mb: 1.5 }}>Llegada, salida y normas</Typography>
                <Typography color="text.secondary">
                  Entrada desde las {formatoHora(aloj.parametros.horaEntrada)} · Salida hasta las {formatoHora(aloj.parametros.horaSalida)}
                </Typography>
                <Box component="ul" sx={{ pl: 2.5, mt: 1, color: 'text.secondary' }}>
                  {aloj.normas.map((n) => <li key={n}>{n}</li>)}
                </Box>
                <Box sx={{ mt: 2 }}>
                  <MapaUbicacion latitud={aloj.latitud} longitud={aloj.longitud} direccion={aloj.direccion} ciudad={aloj.ciudad} />
                </Box>
              </Box>
            )}
          </Stack>
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <Card sx={{ position: { md: 'sticky' }, top: { md: 88 } }}>
            <CardContent>
              <Typography variant="h4" sx={{ mb: 2 }}>Cotizar y reservar</Typography>
              <Stack spacing={2}>
                <Stack direction="row" spacing={1.5}>
                  <CampoFecha label="Llegada" value={entrada} slotProps={{ htmlInput: { min: hoyIso() } }}
                              onChange={(e) => { setEntrada(e.target.value); setCotizacion(null) }} />
                  <CampoFecha label="Salida" value={salida} slotProps={{ htmlInput: { min: entrada } }}
                              onChange={(e) => { setSalida(e.target.value); setCotizacion(null) }} />
                </Stack>
                <Stack direction="row" spacing={1.5}>
                  <CampoHora label="Hora estimada de llegada" value={hora} onChange={(e) => setHora(e.target.value)} />
                  {a.admiteMascotas && (
                    <TextField select label="Mascotas" value={mascotas} sx={{ maxWidth: 120 }}
                               onChange={(e) => { setMascotas(Number(e.target.value)); setCotizacion(null) }}>
                      {[0, 1, 2].map((n) => <MenuItem key={n} value={n}>{n}</MenuItem>)}
                    </TextField>
                  )}
                </Stack>
                <Box>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    {usuario ? `Titular: ${usuario.nombre}. Agregue a quienes lo acompañan.`
                      : `Grupo de la búsqueda: ${adultosBusqueda} adulto(s) y ${menoresBusqueda} menor(es). Al reservar indicará los datos de cada acompañante.`}
                  </Typography>
                  {usuario && <EditorOcupantes ocupantes={acompanantes} maximo={a.capacidad - 1}
                                               alCambiar={(o) => { setAcompanantes(o); setCotizacion(null) }} />}
                </Box>
                <Button variant="outlined" onClick={() => cotizar.mutate()} loading={cotizar.isPending}
                        disabled={!entrada || !salida || acompanantesIncompletos}>
                  Cotizar estancia
                </Button>

                {cotizacion && (
                  <>
                    <Divider />
                    {!cotizacion.disponible && <Alert severity="warning">{cotizacion.mensajeNoDisponible}</Alert>}
                    <DesgloseCotizacion cotizacion={cotizacion.cotizacion} compacto />
                    {aloj && cotizacion.disponible && (
                      <Typography variant="body2" color="text.secondary">
                        Para confirmar se requiere un anticipo del {aloj.parametros.porcentajeAnticipo}%
                        ({formatoPesos(Math.round(cotizacion.cotizacion.valorTotal * aloj.parametros.porcentajeAnticipo / 100))})
                        dentro de las {aloj.parametros.horasPlazoConfirmacion} horas siguientes.
                      </Typography>
                    )}
                  </>
                )}

                {!usuario && (
                  <Button variant="contained" color="secondary" component={RouterLink}
                          to={`/ingresar?volver=${encodeURIComponent(`/apartamentos/${codigo}?entrada=${entrada}&salida=${salida}`)}`}>
                    Ingrese para reservar
                  </Button>
                )}
                {tieneRol('HUESPED') && (
                  <Button variant="contained" color="secondary" size="large" loading={reservar.isPending}
                          disabled={!cotizacion?.disponible || acompanantesIncompletos} onClick={() => reservar.mutate()}>
                    Reservar
                  </Button>
                )}
                {tieneRol('RECEPCIONISTA', 'ADMINISTRADOR') && (
                  <Button variant="contained" color="secondary" component={RouterLink}
                          to={`/recepcion/nueva?apartamento=${codigo}&entrada=${entrada}&salida=${salida}`}>
                    Crear reserva directa
                  </Button>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Container>
  )
}

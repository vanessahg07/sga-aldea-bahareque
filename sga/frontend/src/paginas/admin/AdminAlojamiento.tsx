import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Box, Button, Card, CardContent, Chip, Container, FormControlLabel, Grid, IconButton, InputAdornment, Stack, Switch,
  Tab, Tabs, TextField, Typography,
} from '@mui/material'
import QuitarIcon from '@mui/icons-material/CloseRounded'
import { alojamiento } from '../../api/servicios'
import type { Alojamiento, Parametros, ServicioAdicional } from '../../api/tipos'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, CampoHora, Encabezado, MensajeError } from '../../componentes/Comunes'

/** Datos, normas, parámetros, servicios y medios de pago del alojamiento. Todo es configuración, nada está en el código. */
export function AdminAlojamiento() {
  const consulta = useQuery({ queryKey: ['alojamiento'], queryFn: alojamiento.consultar })
  if (consulta.isLoading) return <Cargando />
  if (consulta.isError) return <Container sx={{ py: 4 }}><MensajeError error={consulta.error} /></Container>
  return <EditorAlojamiento inicial={consulta.data!} />
}

function EditorAlojamiento({ inicial }: { inicial: Alojamiento }) {
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const [pestana, setPestana] = useState(0)
  const [datos, setDatos] = useState<Alojamiento>(inicial)
  const [nuevaNorma, setNuevaNorma] = useState('')
  const [nuevoMedio, setNuevoMedio] = useState('')

  const alGuardar = {
    onSuccess: (a: Alojamiento) => {
      avisos.exito('Cambios guardados')
      clienteConsultas.setQueryData(['alojamiento'], a)
    },
    onError: avisos.error,
  }
  const guardarDatos = useMutation({ mutationFn: () => alojamiento.actualizarDatos(datos), ...alGuardar })
  const guardarParametros = useMutation({ mutationFn: () => alojamiento.cambiarParametros(datos.parametros), ...alGuardar })
  const guardarServicios = useMutation({ mutationFn: () => alojamiento.definirServicios(datos.servicios), ...alGuardar })
  const guardarMedios = useMutation({ mutationFn: () => alojamiento.definirMediosPago(datos.mediosPago), ...alGuardar })


  const p = datos.parametros
  const parametro = (campo: keyof Parametros) => ({
    value: p[campo],
    onChange: (e: { target: { value: string } }) => setDatos({
      ...datos, parametros: { ...p, [campo]: typeof p[campo] === 'number' ? Number(e.target.value) : e.target.value },
    }),
  })
  const servicio = (i: number, cambio: Partial<ServicioAdicional>) =>
    setDatos({ ...datos, servicios: datos.servicios.map((s, j) => (j === i ? { ...s, ...cambio } : s)) })

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Encabezado titulo="Alojamiento" subtitulo="Estos valores son la configuración del negocio: cambiarlos no requiere modificar el sistema." />
      <Tabs value={pestana} onChange={(_, v) => setPestana(v)} sx={{ mb: 3, borderBottom: 1, borderColor: 'divider' }} variant="scrollable">
        <Tab label="Datos y normas" /><Tab label="Parámetros" /><Tab label="Servicios adicionales" /><Tab label="Medios de pago" />
      </Tabs>

      {pestana === 0 && (
        <Card><CardContent>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Nombre" value={datos.nombre} onChange={(e) => setDatos({ ...datos, nombre: e.target.value })} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Ciudad" value={datos.ciudad} onChange={(e) => setDatos({ ...datos, ciudad: e.target.value })} /></Grid>
            <Grid size={12}><TextField label="Descripción" multiline minRows={2} value={datos.descripcion ?? ''} onChange={(e) => setDatos({ ...datos, descripcion: e.target.value })} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Dirección" value={datos.direccion ?? ''} onChange={(e) => setDatos({ ...datos, direccion: e.target.value })} /></Grid>
            <Grid size={{ xs: 6, sm: 3 }}><TextField label="Latitud" type="number" value={datos.latitud} onChange={(e) => setDatos({ ...datos, latitud: Number(e.target.value) })} /></Grid>
            <Grid size={{ xs: 6, sm: 3 }}><TextField label="Longitud" type="number" value={datos.longitud} onChange={(e) => setDatos({ ...datos, longitud: Number(e.target.value) })} /></Grid>
            <Grid size={12}>
              <Typography variant="h6" sx={{ mb: 1 }}>Normas de convivencia</Typography>
              <Stack spacing={1}>
                {datos.normas.map((n, i) => (
                  <Stack key={i} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <TextField value={n} onChange={(e) => setDatos({ ...datos, normas: datos.normas.map((x, j) => (j === i ? e.target.value : x)) })} />
                    <IconButton aria-label="Quitar norma" onClick={() => setDatos({ ...datos, normas: datos.normas.filter((_, j) => j !== i) })}><QuitarIcon /></IconButton>
                  </Stack>
                ))}
                <Stack direction="row" spacing={1}>
                  <TextField label="Nueva norma" value={nuevaNorma} onChange={(e) => setNuevaNorma(e.target.value)} />
                  <Button onClick={() => { if (nuevaNorma.trim()) { setDatos({ ...datos, normas: [...datos.normas, nuevaNorma.trim()] }); setNuevaNorma('') } }}>Agregar</Button>
                </Stack>
              </Stack>
            </Grid>
          </Grid>
          <Button variant="contained" sx={{ mt: 3 }} loading={guardarDatos.isPending} onClick={() => guardarDatos.mutate()}>Guardar datos</Button>
        </CardContent></Card>
      )}

      {pestana === 1 && (
        <Card><CardContent>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 4 }}><TextField label="Umbral de edad facturable" type="number" {...parametro('umbralEdadFacturable')}
                                                      helperText="Los menores de esta edad no pagan, pero cuentan para la capacidad." /></Grid>
            <Grid size={{ xs: 6, sm: 4 }}><CampoHora label="Hora de entrada" {...parametro('horaEntrada')} /></Grid>
            <Grid size={{ xs: 6, sm: 4 }}><CampoHora label="Hora de salida" {...parametro('horaSalida')} /></Grid>
            <Grid size={{ xs: 6, sm: 4 }}><TextField label="Tiempo de preparación (horas)" type="number" {...parametro('horasPreparacion')} /></Grid>
            <Grid size={{ xs: 6, sm: 4 }}><TextField label="Plazo de confirmación (horas)" type="number" {...parametro('horasPlazoConfirmacion')} /></Grid>
            <Grid size={{ xs: 6, sm: 4 }}><CampoHora label="Hora límite de no-show" {...parametro('horaLimiteNoShow')} /></Grid>
            <Grid size={{ xs: 6, sm: 4 }}><TextField label="Anticipo para confirmar" type="number" {...parametro('porcentajeAnticipo')}
                                                     slotProps={{ input: { endAdornment: <InputAdornment position="end">%</InputAdornment> } }} /></Grid>
            <Grid size={{ xs: 6, sm: 4 }}><TextField label="Máximo de mascotas por reserva" type="number" {...parametro('maxMascotas')} /></Grid>
            <Grid size={{ xs: 6, sm: 4 }}><TextField label="Cargo por mascota (estancia)" type="number" {...parametro('cargoMascota')}
                                                     slotProps={{ input: { startAdornment: <InputAdornment position="start">$</InputAdornment> } }} /></Grid>
            <Grid size={{ xs: 6, sm: 4 }}><TextField label="Noches para descuento" type="number" {...parametro('nochesDescuentoLargo')} /></Grid>
            <Grid size={{ xs: 6, sm: 4 }}><TextField label="Descuento por estancia larga" type="number" {...parametro('porcentajeDescuentoLargo')}
                                                     slotProps={{ input: { endAdornment: <InputAdornment position="end">%</InputAdornment> } }} /></Grid>
            <Grid size={{ xs: 6, sm: 4 }}><TextField label="Recordar la llegada con" type="number" {...parametro('diasRecordatorioLlegada')}
                                                     helperText="0 desactiva el recordatorio de llegada"
                                                     slotProps={{ input: { endAdornment: <InputAdornment position="end">días</InputAdornment> } }} /></Grid>
          </Grid>
          <Button variant="contained" sx={{ mt: 3 }} loading={guardarParametros.isPending} onClick={() => guardarParametros.mutate()}>Guardar parámetros</Button>
        </CardContent></Card>
      )}

      {pestana === 2 && (
        <Card><CardContent>
          <Stack spacing={2}>
            {datos.servicios.map((s, i) => (
              <Grid key={i} container spacing={1.5} sx={{ alignItems: 'center', pb: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Grid size={{ xs: 6, sm: 2 }}><TextField label="Código" value={s.codigo} onChange={(e) => servicio(i, { codigo: e.target.value.toUpperCase() })} /></Grid>
                <Grid size={{ xs: 6, sm: 3 }}><TextField label="Nombre" value={s.nombre} onChange={(e) => servicio(i, { nombre: e.target.value })} /></Grid>
                <Grid size={{ xs: 6, sm: 2 }}>
                  <FormControlLabel label="Con cargo" control={<Switch checked={s.generaCargo}
                                    onChange={(e) => servicio(i, { generaCargo: e.target.checked, valor: e.target.checked ? s.valor : 0 })} />} />
                </Grid>
                <Grid size={{ xs: 6, sm: 2 }}><TextField label="Valor" type="number" value={s.valor} disabled={!s.generaCargo}
                                                         onChange={(e) => servicio(i, { valor: Number(e.target.value) })} /></Grid>
                <Grid size={{ xs: 10, sm: 2 }}><TextField label="Unidad" value={s.unidad ?? ''} onChange={(e) => servicio(i, { unidad: e.target.value })} /></Grid>
                <Grid size={{ xs: 2, sm: 1 }}>
                  <IconButton aria-label="Quitar servicio" onClick={() => setDatos({ ...datos, servicios: datos.servicios.filter((_, j) => j !== i) })}><QuitarIcon /></IconButton>
                </Grid>
              </Grid>
            ))}
            <Box>
              <Button onClick={() => setDatos({ ...datos, servicios: [...datos.servicios,
                { codigo: '', nombre: '', descripcion: '', generaCargo: true, valor: 0, unidad: '' }] })}>Agregar servicio</Button>
            </Box>
          </Stack>
          <Button variant="contained" sx={{ mt: 2 }} loading={guardarServicios.isPending} onClick={() => guardarServicios.mutate()}>Guardar servicios</Button>
        </CardContent></Card>
      )}

      {pestana === 3 && (
        <Card><CardContent>
          <Typography color="text.secondary" sx={{ mb: 2 }}>Se requieren al menos dos medios. Los pagos solo se registran con estos medios.</Typography>
          <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1, mb: 2 }}>
            {datos.mediosPago.map((m) => (
              <Chip key={m} label={m} onDelete={() => setDatos({ ...datos, mediosPago: datos.mediosPago.filter((x) => x !== m) })} />
            ))}
          </Stack>
          <Stack direction="row" spacing={1}>
            <TextField label="Nuevo medio" value={nuevoMedio} onChange={(e) => setNuevoMedio(e.target.value.toUpperCase())} sx={{ maxWidth: 260 }} />
            <Button onClick={() => {
              if (nuevoMedio.trim() && !datos.mediosPago.includes(nuevoMedio.trim())) setDatos({ ...datos, mediosPago: [...datos.mediosPago, nuevoMedio.trim()] })
              setNuevoMedio('')
            }}>Agregar</Button>
          </Stack>
          <Button variant="contained" sx={{ mt: 2 }} loading={guardarMedios.isPending} onClick={() => guardarMedios.mutate()}>Guardar medios de pago</Button>
        </CardContent></Card>
      )}
    </Container>
  )
}

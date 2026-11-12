import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Avatar, Button, Card, CardContent, Container, Grid, Stack, TextField, Typography } from '@mui/material'
import { perfil } from '../../api/servicios'
import type { Usuario } from '../../api/tipos'
import { useSesion } from '../../sesion/Sesion'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, Dato, Encabezado, MensajeError } from '../../componentes/Comunes'
import { colores } from '../../tema/tema'
import { nombreRol } from '../../utilidades/estados'
import { formatoFecha } from '../../utilidades/formato'

export function Perfil() {
  const consulta = useQuery({ queryKey: ['perfil'], queryFn: perfil.consultar })
  if (consulta.isLoading) return <Cargando />
  if (consulta.isError) return <Container sx={{ py: 4 }}><MensajeError error={consulta.error} /></Container>
  return <FormularioPerfil usuario={consulta.data!} />
}

function FormularioPerfil({ usuario: u }: { usuario: Usuario }) {
  const { actualizarUsuario } = useSesion()
  const avisos = useAvisos()
  const [datos, setDatos] = useState({ nombre: u.nombre, telefono: u.telefono, fotoUrl: u.fotoUrl ?? '' })
  const [claves, setClaves] = useState({ actual: '', nueva: '' })

  const guardar = useMutation({
    mutationFn: () => perfil.actualizar(datos.nombre, datos.telefono, datos.fotoUrl || null),
    onSuccess: (u) => { actualizarUsuario(u); avisos.exito('Perfil actualizado') },
    onError: avisos.error,
  })
  const cambiarClave = useMutation({
    mutationFn: () => perfil.cambiarContrasena(claves.actual, claves.nueva),
    onSuccess: (r) => { setClaves({ actual: '', nueva: '' }); avisos.exito(r.mensaje) },
    onError: avisos.error,
  })

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Encabezado titulo="Mi perfil" />
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 7 }}>
          <Card>
            <CardContent>
              <Stack direction="row" spacing={2} sx={{ alignItems: 'center', mb: 3 }}>
                <Avatar src={datos.fotoUrl || undefined} sx={{ width: 64, height: 64, bgcolor: colores.teja, fontSize: 26 }}>
                  {u.nombre.charAt(0)}
                </Avatar>
                <Stack>
                  <Typography variant="h5">{u.nombre}</Typography>
                  <Typography color="text.secondary">{nombreRol[u.rol]}</Typography>
                </Stack>
              </Stack>
              <Stack spacing={2}>
                <TextField label="Nombre" value={datos.nombre} onChange={(e) => setDatos({ ...datos, nombre: e.target.value })} />
                <TextField label="Teléfono" value={datos.telefono} onChange={(e) => setDatos({ ...datos, telefono: e.target.value })} />
                <TextField label="URL de la foto de perfil" value={datos.fotoUrl} onChange={(e) => setDatos({ ...datos, fotoUrl: e.target.value })} />
                <Grid container spacing={2}>
                  <Grid size={6}><Dato etiqueta="Correo (no editable)">{u.correo}</Dato></Grid>
                  <Grid size={6}><Dato etiqueta="Documento">{u.documento}</Dato></Grid>
                  <Grid size={6}><Dato etiqueta="Fecha de nacimiento">{formatoFecha(u.fechaNacimiento)}</Dato></Grid>
                </Grid>
                <Button variant="contained" onClick={() => guardar.mutate()} loading={guardar.isPending}
                        disabled={!datos.nombre.trim() || !datos.telefono.trim()} sx={{ alignSelf: 'flex-start' }}>
                  Guardar cambios
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 5 }}>
          <Card>
            <CardContent>
              <Typography variant="h5" sx={{ mb: 2 }}>Cambiar contraseña</Typography>
              <Stack spacing={2}>
                <TextField label="Contraseña actual" type="password" value={claves.actual}
                           onChange={(e) => setClaves({ ...claves, actual: e.target.value })} />
                <TextField label="Nueva contraseña" type="password" value={claves.nueva}
                           onChange={(e) => setClaves({ ...claves, nueva: e.target.value })}
                           helperText="Mínimo 8 caracteres, con al menos una mayúscula y un número." />
                <Button variant="outlined" onClick={() => cambiarClave.mutate()} loading={cambiarClave.isPending}
                        disabled={!claves.actual || !claves.nueva}>
                  Cambiar contraseña
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Container>
  )
}

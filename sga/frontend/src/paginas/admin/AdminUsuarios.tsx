import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button, Card, Container, Dialog, DialogActions, DialogContent, DialogTitle, Grid, IconButton, MenuItem, Table,
  TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Tooltip,
} from '@mui/material'
import BorrarIcon from '@mui/icons-material/PersonRemoveOutlined'
import { usuarios, type DatosRegistro } from '../../api/servicios'
import type { Rol, Usuario } from '../../api/tipos'
import { useSesion } from '../../sesion/Sesion'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, CampoFecha, Confirmacion, Encabezado, MensajeError, Paginador } from '../../componentes/Comunes'
import { nombreRol, ROLES } from '../../utilidades/estados'

const vacio: DatosRegistro & { rol: Rol } = {
  nombre: '', correo: '', contrasena: '', telefono: '', documento: '', fechaNacimiento: '', rol: 'RECEPCIONISTA',
}

/** Gestión de usuarios y roles. Solo el administrador asigna roles internos; la eliminación es lógica. */
export function AdminUsuarios() {
  const { usuario: yo } = useSesion()
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const [pagina, setPagina] = useState(0)
  const [nuevo, setNuevo] = useState<typeof vacio | null>(null)
  const [eliminar, setEliminar] = useState<Usuario | null>(null)
  const consulta = useQuery({ queryKey: ['usuarios', pagina], queryFn: () => usuarios.listar(pagina) })

  const refrescar = (mensaje: string) => () => {
    avisos.exito(mensaje)
    setNuevo(null)
    setEliminar(null)
    clienteConsultas.invalidateQueries({ queryKey: ['usuarios'] })
  }
  const crear = useMutation({ mutationFn: () => usuarios.crear(nuevo!), onSuccess: refrescar('Usuario creado'), onError: avisos.error })
  const cambiarRol = useMutation({
    mutationFn: ({ id, rol }: { id: string; rol: Rol }) => usuarios.asignarRol(id, rol),
    onSuccess: refrescar('Rol actualizado'), onError: avisos.error,
  })
  const borrar = useMutation({ mutationFn: (id: string) => usuarios.eliminar(id), onSuccess: refrescar('Usuario eliminado'), onError: avisos.error })

  const campo = (clave: keyof typeof vacio) => ({
    value: nuevo?.[clave] ?? '',
    onChange: (e: { target: { value: string } }) => nuevo && setNuevo({ ...nuevo, [clave]: e.target.value }),
  })

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Encabezado titulo="Usuarios" subtitulo="El registro público crea huéspedes; aquí se crean las cuentas del personal."
                  acciones={<Button variant="contained" color="secondary" onClick={() => setNuevo(vacio)}>Nuevo usuario</Button>} />
      {consulta.isLoading && <Cargando />}
      {consulta.isError && <MensajeError error={consulta.error} />}
      {consulta.data && (
        <Card>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow><TableCell>Nombre</TableCell><TableCell>Correo</TableCell><TableCell>Documento</TableCell><TableCell>Teléfono</TableCell><TableCell>Rol</TableCell><TableCell /></TableRow>
              </TableHead>
              <TableBody>
                {consulta.data.contenido.map((u) => (
                  <TableRow key={u.id}>
                    <TableCell sx={{ fontWeight: 600 }}>{u.nombre}</TableCell>
                    <TableCell>{u.correo}</TableCell>
                    <TableCell>{u.documento}</TableCell>
                    <TableCell>{u.telefono}</TableCell>
                    <TableCell sx={{ minWidth: 190 }}>
                      <TextField select value={u.rol} disabled={u.id === yo?.id}
                                 onChange={(e) => cambiarRol.mutate({ id: u.id, rol: e.target.value as Rol })}>
                        {ROLES.map((r) => <MenuItem key={r} value={r}>{nombreRol[r]}</MenuItem>)}
                      </TextField>
                    </TableCell>
                    <TableCell align="right">
                      {u.id !== yo?.id && (
                        <Tooltip title="Eliminar usuario"><IconButton onClick={() => setEliminar(u)}><BorrarIcon /></IconButton></Tooltip>
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

      <Dialog open={!!nuevo} onClose={() => setNuevo(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Nuevo usuario</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ pt: 1 }}>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Nombre" {...campo('nombre')} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Correo" type="email" {...campo('correo')} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Documento" {...campo('documento')} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Teléfono" {...campo('telefono')} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><CampoFecha label="Fecha de nacimiento" {...campo('fechaNacimiento')} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField select label="Rol" {...campo('rol')}>{ROLES.map((r) => <MenuItem key={r} value={r}>{nombreRol[r]}</MenuItem>)}</TextField>
            </Grid>
            <Grid size={12}><TextField label="Contraseña inicial" type="password" {...campo('contrasena')}
                                       helperText="Mínimo 8 caracteres, con al menos una mayúscula y un número." /></Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button color="inherit" onClick={() => setNuevo(null)}>Cancelar</Button>
          <Button variant="contained" loading={crear.isPending} onClick={() => crear.mutate()}>Crear usuario</Button>
        </DialogActions>
      </Dialog>
      <Confirmacion abierto={!!eliminar} titulo={`Eliminar a ${eliminar?.nombre ?? ''}`} peligrosa textoConfirmar="Eliminar"
                    cargando={borrar.isPending} alCerrar={() => setEliminar(null)} alConfirmar={() => eliminar && borrar.mutate(eliminar.id)}>
        La cuenta deja de poder ingresar. Sus reservas se conservan. No se puede eliminar a quien sea titular de reservas activas.
      </Confirmacion>
    </Container>
  )
}

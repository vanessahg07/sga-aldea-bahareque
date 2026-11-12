import { useState, type FormEvent, type ReactNode } from 'react'
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router'
import { useMutation } from '@tanstack/react-query'
import { Alert, Box, Button, Card, CardContent, Container, Link, Stack, TextField, Typography } from '@mui/material'
import { autenticacion } from '../../api/servicios'
import { mensajeDe } from '../../api/cliente'
import type { Sesion } from '../../api/tipos'
import { inicioPara, useSesion } from '../../sesion/Sesion'
import { useAvisos } from '../../componentes/Avisos'
import { CampoFecha } from '../../componentes/Comunes'

function Marco({ titulo, subtitulo, children }: { titulo: string; subtitulo: string; children: ReactNode }) {
  return (
    <Container maxWidth="sm" sx={{ py: { xs: 4, md: 8 } }}>
      <Card>
        <CardContent sx={{ p: { xs: 3, md: 5 } }}>
          <Typography variant="h3" component="h1">{titulo}</Typography>
          <Typography color="text.secondary" sx={{ mt: 1, mb: 3 }}>{subtitulo}</Typography>
          {children}
        </CardContent>
      </Card>
    </Container>
  )
}

function useAlIngresar() {
  const { iniciar } = useSesion()
  const navegar = useNavigate()
  const [parametros] = useSearchParams()
  return (sesion: Sesion) => {
    iniciar(sesion)
    navegar(parametros.get('volver') ?? inicioPara(sesion.usuario.rol), { replace: true })
  }
}

/** Contraseña segura: mínimo 8 caracteres, con una mayúscula y un número (misma regla que el dominio). */
function contrasenaSegura(valor: string) {
  return valor.length >= 8 && /[A-Z]/.test(valor) && /\d/.test(valor)
}

export function Ingresar() {
  const alIngresar = useAlIngresar()
  const [parametros] = useSearchParams()
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const ingreso = useMutation({ mutationFn: () => autenticacion.ingresar(correo, contrasena), onSuccess: alIngresar })

  const enviar = (e: FormEvent) => {
    e.preventDefault()
    ingreso.mutate()
  }

  return (
    <Marco titulo="Ingresar" subtitulo="Consulte sus reservas o gestione el alojamiento.">
      <Stack component="form" spacing={2} onSubmit={enviar}>
        {ingreso.isError && <Alert severity="error">{mensajeDe(ingreso.error)}</Alert>}
        <TextField label="Correo electrónico" type="email" autoComplete="email" required value={correo}
                   onChange={(e) => setCorreo(e.target.value)} />
        <TextField label="Contraseña" type="password" autoComplete="current-password" required value={contrasena}
                   onChange={(e) => setContrasena(e.target.value)} />
        <Button type="submit" variant="contained" size="large" loading={ingreso.isPending}>Ingresar</Button>
        <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
          <Link component={RouterLink} to="/recuperar">¿Olvidó su contraseña?</Link>
          <Link component={RouterLink} to={`/registro${parametros.get('volver') ? `?volver=${encodeURIComponent(parametros.get('volver')!)}` : ''}`}>
            Crear cuenta
          </Link>
        </Stack>
      </Stack>
    </Marco>
  )
}

export function Registro() {
  const alIngresar = useAlIngresar()
  const [datos, setDatos] = useState({ nombre: '', correo: '', contrasena: '', telefono: '', documento: '', fechaNacimiento: '' })
  const [intentado, setIntentado] = useState(false)
  const registro = useMutation({ mutationFn: () => autenticacion.registrar(datos), onSuccess: alIngresar })
  const cambiar = (campo: keyof typeof datos) => (e: { target: { value: string } }) => setDatos({ ...datos, [campo]: e.target.value })
  const contrasenaInvalida = intentado && !contrasenaSegura(datos.contrasena)

  const enviar = (e: FormEvent) => {
    e.preventDefault()
    setIntentado(true)
    if (contrasenaSegura(datos.contrasena)) registro.mutate()
  }

  return (
    <Marco titulo="Crear cuenta" subtitulo="Reserve en línea y consulte el estado y el folio de sus reservas.">
      <Stack component="form" spacing={2} onSubmit={enviar}>
        {registro.isError && <Alert severity="error">{mensajeDe(registro.error)}</Alert>}
        <TextField label="Nombre completo" required value={datos.nombre} onChange={cambiar('nombre')} />
        <TextField label="Correo electrónico" type="email" required value={datos.correo} onChange={cambiar('correo')} />
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField label="Documento de identidad" required value={datos.documento} onChange={cambiar('documento')} />
          <TextField label="Teléfono" required value={datos.telefono} onChange={cambiar('telefono')} />
        </Stack>
        <CampoFecha label="Fecha de nacimiento" required value={datos.fechaNacimiento} onChange={cambiar('fechaNacimiento')} />
        <TextField label="Contraseña" type="password" required value={datos.contrasena} onChange={cambiar('contrasena')}
                   error={contrasenaInvalida}
                   helperText="Mínimo 8 caracteres, con al menos una mayúscula y un número." />
        <Button type="submit" variant="contained" size="large" loading={registro.isPending}>Crear cuenta</Button>
        <Typography variant="body2" color="text.secondary">
          ¿Ya tiene cuenta? <Link component={RouterLink} to="/ingresar">Ingrese aquí</Link>
        </Typography>
      </Stack>
    </Marco>
  )
}

export function Recuperar() {
  const navegar = useNavigate()
  const avisos = useAvisos()
  const [correo, setCorreo] = useState('')
  const [codigo, setCodigo] = useState('')
  const [nueva, setNueva] = useState('')
  const [enviado, setEnviado] = useState(false)

  const solicitud = useMutation({ mutationFn: () => autenticacion.solicitarCodigo(correo), onSuccess: () => setEnviado(true) })
  const cambio = useMutation({
    mutationFn: () => autenticacion.restablecer(correo, codigo, nueva),
    onSuccess: (r) => { avisos.exito(r.mensaje); navegar('/ingresar') },
  })

  return (
    <Marco titulo="Recuperar contraseña"
           subtitulo="Le enviaremos un código de 6 dígitos. Es válido por 15 minutos y sirve una sola vez.">
      {!enviado ? (
        <Stack component="form" spacing={2} onSubmit={(e) => { e.preventDefault(); solicitud.mutate() }}>
          {solicitud.isError && <Alert severity="error">{mensajeDe(solicitud.error)}</Alert>}
          <TextField label="Correo electrónico" type="email" required value={correo} onChange={(e) => setCorreo(e.target.value)} />
          <Button type="submit" variant="contained" loading={solicitud.isPending}>Enviar código</Button>
        </Stack>
      ) : (
        <Stack component="form" spacing={2} onSubmit={(e) => {
          e.preventDefault()
          if (contrasenaSegura(nueva)) cambio.mutate()
        }}>
          <Alert severity="info">Si {correo} está registrado, recibirá el código en unos segundos.</Alert>
          {cambio.isError && <Alert severity="error">{mensajeDe(cambio.error)}</Alert>}
          <TextField label="Código" required value={codigo} onChange={(e) => setCodigo(e.target.value)}
                     slotProps={{ htmlInput: { inputMode: 'numeric', maxLength: 6 } }} />
          <TextField label="Nueva contraseña" type="password" required value={nueva} onChange={(e) => setNueva(e.target.value)}
                     error={!!nueva && !contrasenaSegura(nueva)} helperText="Mínimo 8 caracteres, con al menos una mayúscula y un número." />
          <Button type="submit" variant="contained" loading={cambio.isPending}>Cambiar contraseña</Button>
          <Box><Button size="small" onClick={() => setEnviado(false)}>Usar otro correo</Button></Box>
        </Stack>
      )}
    </Marco>
  )
}

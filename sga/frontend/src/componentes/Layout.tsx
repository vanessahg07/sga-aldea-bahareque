import { useState } from 'react'
import { Link as RouterLink, NavLink, Outlet, useLocation, useNavigate } from 'react-router'
import {
  AppBar, Avatar, Box, Button, Container, Divider, IconButton, Link, ListItemIcon, Menu, MenuItem, Stack, Toolbar,
  Typography,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/MenuRounded'
import AbajoIcon from '@mui/icons-material/ExpandMoreRounded'
import SalirIcon from '@mui/icons-material/LogoutOutlined'
import PerfilIcon from '@mui/icons-material/PersonOutlined'
import type { Rol } from '../api/tipos'
import { useSesion } from '../sesion/Sesion'
import { colores } from '../tema/tema'
import { nombreRol } from '../utilidades/estados'

interface Enlace {
  a: string
  texto: string
  roles?: Rol[]
  /** Los enlaces de administración se agrupan en un menú para no saturar la barra. */
  administracion?: boolean
}

const ENLACES: Enlace[] = [
  { a: '/', texto: 'Apartamentos' },
  { a: '/mis-reservas', texto: 'Mis reservas', roles: ['HUESPED'] },
  { a: '/recepcion', texto: 'Hoy', roles: ['RECEPCIONISTA', 'ADMINISTRADOR'] },
  { a: '/recepcion/reservas', texto: 'Reservas', roles: ['RECEPCIONISTA', 'ADMINISTRADOR'] },
  { a: '/recepcion/ocupacion', texto: 'Ocupación', roles: ['RECEPCIONISTA', 'ADMINISTRADOR'] },
  { a: '/servicio', texto: 'Preparación', roles: ['PERSONAL_SERVICIO', 'ADMINISTRADOR'] },
  { a: '/admin/reportes', texto: 'Reportes', roles: ['ADMINISTRADOR'], administracion: true },
  { a: '/admin/apartamentos', texto: 'Inventario', roles: ['ADMINISTRADOR'], administracion: true },
  { a: '/admin/tarifas', texto: 'Tarifas', roles: ['ADMINISTRADOR'], administracion: true },
  { a: '/admin/alojamiento', texto: 'Alojamiento', roles: ['ADMINISTRADOR'], administracion: true },
  { a: '/admin/canales', texto: 'Canales de venta', roles: ['ADMINISTRADOR'], administracion: true },
  { a: '/admin/recordatorios', texto: 'Recordatorios', roles: ['ADMINISTRADOR'], administracion: true },
  { a: '/admin/usuarios', texto: 'Usuarios', roles: ['ADMINISTRADOR'], administracion: true },
]

const estiloEnlace = {
  opacity: 0.85,
  whiteSpace: 'nowrap',
  '&.active': { opacity: 1, boxShadow: `inset 0 -3px 0 ${colores.guadua}`, borderRadius: 0 },
}

function Marca() {
  return (
    <Stack component={RouterLink} to="/" direction="row" spacing={1.25}
           sx={{ alignItems: 'center', color: 'inherit', textDecoration: 'none', mr: 3, whiteSpace: 'nowrap', flexShrink: 0 }}>
      <Box component="img" src="/favicon.svg" alt="" sx={{ width: 30, height: 30 }} />
      <Box>
        <Typography sx={{ fontWeight: 700, fontSize: 19, lineHeight: 1 }}>
          Aldea Bahareque
        </Typography>
        <Typography sx={{ fontSize: 11, opacity: 0.8, letterSpacing: '0.08em' }}>FILANDIA · QUINDÍO</Typography>
      </Box>
    </Stack>
  )
}

export function Layout() {
  const { usuario, cerrar, tieneRol } = useSesion()
  const navegar = useNavigate()
  const [menuUsuario, setMenuUsuario] = useState<HTMLElement | null>(null)
  const [menuMovil, setMenuMovil] = useState<HTMLElement | null>(null)
  const [menuAdmin, setMenuAdmin] = useState<HTMLElement | null>(null)
  const ubicacion = useLocation()
  const visibles = ENLACES.filter((e) => !e.roles || tieneRol(...e.roles))
  const principales = visibles.filter((e) => !e.administracion)
  const administracion = visibles.filter((e) => e.administracion)
  const enAdministracion = ubicacion.pathname.startsWith('/admin')

  const salir = () => {
    setMenuUsuario(null)
    cerrar()
    navegar('/')
  }

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="sticky" color="primary">
        <Container maxWidth="xl">
          <Toolbar disableGutters sx={{ gap: 1 }}>
            <Marca />
            <Stack direction="row" spacing={0.5} sx={{ flexGrow: 1, display: { xs: 'none', lg: 'flex' } }}>
              {principales.map((e) => (
                <Button key={e.a} component={NavLink} to={e.a} end color="inherit" sx={estiloEnlace}>
                  {e.texto}
                </Button>
              ))}
              {administracion.length > 0 && (
                <>
                  <Button color="inherit" endIcon={<AbajoIcon />} onClick={(e) => setMenuAdmin(e.currentTarget)}
                          className={enAdministracion ? 'active' : undefined} sx={estiloEnlace}>
                    Administración
                  </Button>
                  <Menu anchorEl={menuAdmin} open={!!menuAdmin} onClose={() => setMenuAdmin(null)}>
                    {administracion.map((e) => (
                      <MenuItem key={e.a} selected={ubicacion.pathname.startsWith(e.a)}
                                onClick={() => { setMenuAdmin(null); navegar(e.a) }}>{e.texto}</MenuItem>
                    ))}
                  </Menu>
                </>
              )}
            </Stack>
            <Box sx={{ flexGrow: 1, display: { xs: 'flex', lg: 'none' } }}>
              <IconButton color="inherit" aria-label="Abrir menú" onClick={(e) => setMenuMovil(e.currentTarget)}><MenuIcon /></IconButton>
              <Menu anchorEl={menuMovil} open={!!menuMovil} onClose={() => setMenuMovil(null)}>
                {visibles.map((e) => (
                  <MenuItem key={e.a} onClick={() => { setMenuMovil(null); navegar(e.a) }}>{e.texto}</MenuItem>
                ))}
              </Menu>
            </Box>
            {usuario ? (
              <>
                <Button color="inherit" onClick={(e) => setMenuUsuario(e.currentTarget)}
                        startIcon={<Avatar src={usuario.fotoUrl ?? undefined}
                                           sx={{ width: 28, height: 28, bgcolor: colores.teja, fontSize: 14 }}>
                          {usuario.nombre.charAt(0)}
                        </Avatar>}>
                  <Box component="span" sx={{ display: { xs: 'none', sm: 'inline' } }}>{usuario.nombre.split(' ')[0]}</Box>
                </Button>
                <Menu anchorEl={menuUsuario} open={!!menuUsuario} onClose={() => setMenuUsuario(null)}>
                  <MenuItem disabled sx={{ opacity: '1 !important' }}>
                    <Box>
                      <Typography sx={{ fontWeight: 600 }}>{usuario.nombre}</Typography>
                      <Typography variant="caption" color="text.secondary">{nombreRol[usuario.rol]} · {usuario.correo}</Typography>
                    </Box>
                  </MenuItem>
                  <Divider />
                  <MenuItem onClick={() => { setMenuUsuario(null); navegar('/perfil') }}>
                    <ListItemIcon><PerfilIcon fontSize="small" /></ListItemIcon>Mi perfil
                  </MenuItem>
                  <MenuItem onClick={salir}>
                    <ListItemIcon><SalirIcon fontSize="small" /></ListItemIcon>Cerrar sesión
                  </MenuItem>
                </Menu>
              </>
            ) : (
              <Stack direction="row" spacing={1}>
                <Button color="inherit" component={RouterLink} to="/ingresar">Ingresar</Button>
                <Button variant="contained" color="secondary" component={RouterLink} to="/registro">Crear cuenta</Button>
              </Stack>
            )}
          </Toolbar>
        </Container>
      </AppBar>

      <Box component="main" sx={{ flexGrow: 1 }}>
        <Outlet />
      </Box>

      <Box component="footer" sx={{ borderTop: 1, borderColor: 'divider', py: 3, mt: 6 }}>
        <Container maxWidth="xl">
          <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Aldea Bahareque · Apartamentos turísticos en Filandia, Quindío
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Proyecto SGA · Programación Avanzada · <Link component={RouterLink} to="/politica" color="inherit">Política de cancelación</Link>
            </Typography>
          </Stack>
        </Container>
      </Box>
    </Box>
  )
}

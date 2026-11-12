import { lazy, Suspense, type ComponentType, type ReactNode } from 'react'
import { createBrowserRouter } from 'react-router'
import { Layout } from './componentes/Layout'
import { Cargando } from './componentes/Comunes'
import { RutaProtegida } from './sesion/RutaProtegida'
import type { Rol } from './api/tipos'
import { Inicio } from './paginas/publico/Inicio'

/** Carga diferida por página: el portal público no descarga las pantallas internas. */
function diferida<M>(cargar: () => Promise<M>, nombre: keyof M) {
  const Componente = lazy(async () => ({ default: (await cargar())[nombre] as ComponentType }))
  return (
    <Suspense fallback={<Cargando />}>
      <Componente />
    </Suspense>
  )
}

const protegida = (contenido: ReactNode, roles?: Rol[]) => <RutaProtegida roles={roles}>{contenido}</RutaProtegida>
const PERSONAL: Rol[] = ['RECEPCIONISTA', 'ADMINISTRADOR']
const ADMIN: Rol[] = ['ADMINISTRADOR']

export const enrutador = createBrowserRouter([
  {
    element: <Layout />,
    children: [
      { path: '/', element: <Inicio /> },
      { path: '/apartamentos/:codigo', element: diferida(() => import('./paginas/publico/DetalleApartamento'), 'DetalleApartamento') },
      { path: '/politica', element: diferida(() => import('./paginas/publico/PoliticaPublica'), 'PoliticaPublica') },
      { path: '/ingresar', element: diferida(() => import('./paginas/publico/Acceso'), 'Ingresar') },
      { path: '/registro', element: diferida(() => import('./paginas/publico/Acceso'), 'Registro') },
      { path: '/recuperar', element: diferida(() => import('./paginas/publico/Acceso'), 'Recuperar') },
      { path: '/mis-reservas', element: protegida(diferida(() => import('./paginas/huesped/MisReservas'), 'MisReservas'), ['HUESPED']) },
      { path: '/perfil', element: protegida(diferida(() => import('./paginas/huesped/Perfil'), 'Perfil')) },
      { path: '/reservas/:id', element: protegida(diferida(() => import('./paginas/reserva/DetalleReserva'), 'DetalleReserva')) },
      { path: '/recepcion', element: protegida(diferida(() => import('./paginas/recepcion/TableroRecepcion'), 'TableroRecepcion'), PERSONAL) },
      { path: '/recepcion/reservas', element: protegida(diferida(() => import('./paginas/recepcion/ListadoReservas'), 'ListadoReservas'), PERSONAL) },
      { path: '/recepcion/nueva', element: protegida(diferida(() => import('./paginas/recepcion/NuevaReservaDirecta'), 'NuevaReservaDirecta'), PERSONAL) },
      { path: '/recepcion/ocupacion', element: protegida(diferida(() => import('./paginas/recepcion/Ocupacion'), 'Ocupacion'), PERSONAL) },
      { path: '/servicio', element: protegida(diferida(() => import('./paginas/servicio/Preparacion'), 'Preparacion'), ['PERSONAL_SERVICIO', 'ADMINISTRADOR']) },
      { path: '/admin/apartamentos', element: protegida(diferida(() => import('./paginas/admin/AdminApartamentos'), 'AdminApartamentos'), ADMIN) },
      { path: '/admin/apartamentos/:codigo', element: protegida(diferida(() => import('./paginas/admin/AdminApartamento'), 'AdminApartamento'), ADMIN) },
      { path: '/admin/tarifas', element: protegida(diferida(() => import('./paginas/admin/AdminTarifas'), 'AdminTarifas'), ADMIN) },
      { path: '/admin/alojamiento', element: protegida(diferida(() => import('./paginas/admin/AdminAlojamiento'), 'AdminAlojamiento'), ADMIN) },
      { path: '/admin/usuarios', element: protegida(diferida(() => import('./paginas/admin/AdminUsuarios'), 'AdminUsuarios'), ADMIN) },
      { path: '/admin/canales', element: protegida(diferida(() => import('./paginas/admin/AdminCanales'), 'AdminCanales'), ADMIN) },
      { path: '/admin/reportes', element: protegida(diferida(() => import('./paginas/admin/AdminReportes'), 'AdminReportes'), ADMIN) },
      { path: '/admin/recordatorios', element: protegida(diferida(() => import('./paginas/admin/AdminRecordatorios'), 'AdminRecordatorios'), ADMIN) },
      { path: '*', element: diferida(() => import('./paginas/publico/NoEncontrada'), 'NoEncontrada') },
    ],
  },
])

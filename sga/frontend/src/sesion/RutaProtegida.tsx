import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { Alert, Container } from '@mui/material'
import type { Rol } from '../api/tipos'
import { useSesion } from './Sesion'

/** Exige sesión iniciada y, si se indican, alguno de los roles. */
export function RutaProtegida({ roles, children }: { roles?: Rol[]; children: ReactNode }) {
  const { usuario, tieneRol } = useSesion()
  const ubicacion = useLocation()

  if (!usuario) {
    return <Navigate to={`/ingresar?volver=${encodeURIComponent(ubicacion.pathname + ubicacion.search)}`} replace />
  }
  if (roles && !tieneRol(...roles)) {
    return (
      <Container sx={{ py: 6 }}>
        <Alert severity="warning">No tiene permiso para ver esta sección.</Alert>
      </Container>
    )
  }
  return <>{children}</>
}

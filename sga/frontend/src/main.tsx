import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router'
import { tema } from './tema/tema'
import { ProveedorSesion } from './sesion/Sesion'
import { ProveedorAvisos } from './componentes/Avisos'
import { enrutador } from './rutas'
import { ErrorApi } from './api/cliente'

const clienteConsultas = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: (intentos, error) => !(error instanceof ErrorApi && error.estado >= 400 && error.estado < 500) && intentos < 2,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={tema}>
      <CssBaseline />
      <QueryClientProvider client={clienteConsultas}>
        <ProveedorSesion>
          <ProveedorAvisos>
            <RouterProvider router={enrutador} />
          </ProveedorAvisos>
        </ProveedorSesion>
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
)

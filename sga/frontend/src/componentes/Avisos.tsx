import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'
import { Alert, Snackbar } from '@mui/material'
import { mensajeDe } from '../api/cliente'

interface Aviso {
  id: number
  texto: string
  tipo: 'success' | 'error' | 'info'
}

interface ContextoAvisos {
  exito: (texto: string) => void
  error: (error: unknown) => void
  info: (texto: string) => void
}

const Contexto = createContext<ContextoAvisos | null>(null)

/** Mensajes breves de confirmación y de error, con el significado de negocio que devuelve la API. */
export function ProveedorAvisos({ children }: { children: ReactNode }) {
  const [aviso, setAviso] = useState<Aviso | null>(null)

  const mostrar = useCallback((texto: string, tipo: Aviso['tipo']) => setAviso({ id: Date.now(), texto, tipo }), [])
  const valor: ContextoAvisos = {
    exito: useCallback((texto: string) => mostrar(texto, 'success'), [mostrar]),
    error: useCallback((error: unknown) => mostrar(mensajeDe(error), 'error'), [mostrar]),
    info: useCallback((texto: string) => mostrar(texto, 'info'), [mostrar]),
  }

  return (
    <Contexto.Provider value={valor}>
      {children}
      <Snackbar key={aviso?.id} open={!!aviso} autoHideDuration={aviso?.tipo === 'error' ? 8000 : 4000}
                onClose={() => setAviso(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        {aviso ? (
          <Alert severity={aviso.tipo} variant="filled" onClose={() => setAviso(null)} sx={{ maxWidth: 560 }}>
            {aviso.texto}
          </Alert>
        ) : undefined}
      </Snackbar>
    </Contexto.Provider>
  )
}

export function useAvisos(): ContextoAvisos {
  const contexto = useContext(Contexto)
  if (!contexto) throw new Error('useAvisos debe usarse dentro de ProveedorAvisos')
  return contexto
}

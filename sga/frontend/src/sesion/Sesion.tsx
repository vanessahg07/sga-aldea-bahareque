import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { configurarSesion } from '../api/cliente'
import type { Rol, Sesion, Usuario } from '../api/tipos'

const CLAVE = 'sga.sesion'

interface ContextoSesion {
  usuario: Usuario | null
  iniciar: (sesion: Sesion) => void
  actualizarUsuario: (usuario: Usuario) => void
  cerrar: () => void
  tieneRol: (...roles: Rol[]) => boolean
}

const Contexto = createContext<ContextoSesion | null>(null)

function leerGuardada(): Sesion | null {
  try {
    const texto = localStorage.getItem(CLAVE)
    if (!texto) return null
    const sesion = JSON.parse(texto) as Sesion
    return new Date(sesion.expiraEn).getTime() > Date.now() ? sesion : null
  } catch {
    return null
  }
}

function guardar(sesion: Sesion | null) {
  try {
    if (sesion) localStorage.setItem(CLAVE, JSON.stringify(sesion))
    else localStorage.removeItem(CLAVE)
  } catch {
    // Sin almacenamiento local la sesión dura mientras la pestaña esté abierta.
  }
}

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const clienteConsultas = useQueryClient()
  const [sesion, setSesion] = useState<Sesion | null>(() => {
    const guardada = leerGuardada()
    configurarSesion(guardada?.token ?? null, null)
    return guardada
  })

  const cerrar = useCallback(() => {
    guardar(null)
    configurarSesion(null, null)
    setSesion(null)
    clienteConsultas.clear()
  }, [clienteConsultas])

  const iniciar = useCallback((nueva: Sesion) => {
    guardar(nueva)
    configurarSesion(nueva.token, cerrar)
    clienteConsultas.clear()
    setSesion(nueva)
  }, [cerrar, clienteConsultas])

  const actualizarUsuario = useCallback((usuario: Usuario) => {
    setSesion((actual) => {
      if (!actual) return actual
      const nueva = { ...actual, usuario }
      guardar(nueva)
      return nueva
    })
  }, [])

  if (sesion) configurarSesion(sesion.token, cerrar)

  const valor = useMemo<ContextoSesion>(() => ({
    usuario: sesion?.usuario ?? null,
    iniciar,
    actualizarUsuario,
    cerrar,
    tieneRol: (...roles) => !!sesion && roles.includes(sesion.usuario.rol),
  }), [sesion, iniciar, actualizarUsuario, cerrar])

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useSesion(): ContextoSesion {
  const contexto = useContext(Contexto)
  if (!contexto) throw new Error('useSesion debe usarse dentro de ProveedorSesion')
  return contexto
}

/** Ruta de inicio según el rol. */
export function inicioPara(rol: Rol | undefined): string {
  switch (rol) {
    case 'RECEPCIONISTA': return '/recepcion'
    case 'ADMINISTRADOR': return '/recepcion'
    case 'PERSONAL_SERVICIO': return '/servicio'
    default: return '/'
  }
}

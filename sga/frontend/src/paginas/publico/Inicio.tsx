import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import {
  Box, Button, Card, CardContent, Chip, Container, Grid, InputAdornment, MenuItem, Stack, TextField, Typography,
} from '@mui/material'
import BuscarIcon from '@mui/icons-material/SearchRounded'
import { alojamiento, apartamentos, disponibilidad } from '../../api/servicios'
import { Cargando, CampoFecha, MensajeError, Paginador, Vacio } from '../../componentes/Comunes'
import { TarjetaApartamento } from '../../componentes/Negocio'
import { colores } from '../../tema/tema'
import { etiqueta, fechasDelGrupo, hoyIso, sumarDias } from '../../utilidades/formato'

export function Inicio() {
  const navegar = useNavigate()
  const [parametros, setParametros] = useSearchParams()
  const entrada = parametros.get('entrada') ?? sumarDias(hoyIso(), 14)
  const salida = parametros.get('salida') ?? sumarDias(hoyIso(), 16)
  const adultos = Number(parametros.get('adultos') ?? 2)
  const menores = Number(parametros.get('menores') ?? 0)
  const mascotas = Number(parametros.get('mascotas') ?? 0)
  const capacidadMinima = parametros.get('capacidad')
  const precioMaximo = parametros.get('precioMax')
  const caracteristicas = parametros.getAll('caracteristica')
  const pagina = Number(parametros.get('pagina') ?? 0)

  const [borrador, setBorrador] = useState({ entrada, salida, adultos, menores, mascotas })

  const datosAlojamiento = useQuery({ queryKey: ['alojamiento'], queryFn: alojamiento.consultar })
  const inventario = useQuery({ queryKey: ['apartamentos', 'venta'], queryFn: () => apartamentos.aLaVenta(0) })
  const umbral = datosAlojamiento.data?.parametros.umbralEdadFacturable

  const todasLasCaracteristicas = useMemo(() => {
    const conjunto = new Set<string>()
    inventario.data?.contenido.forEach((a) => a.caracteristicas.forEach((c) => conjunto.add(c)))
    return [...conjunto].sort()
  }, [inventario.data])

  const resultados = useQuery({
    queryKey: ['busqueda', entrada, salida, adultos, menores, mascotas, capacidadMinima, precioMaximo, caracteristicas.join(','), pagina],
    queryFn: () => disponibilidad.buscar({
      entrada, salida,
      fechasNacimiento: fechasDelGrupo(adultos, menores, entrada),
      capacidadMinima: capacidadMinima ? Number(capacidadMinima) : null,
      precioMaximo: precioMaximo ? Number(precioMaximo) : null,
      caracteristicas, mascotas,
    }, pagina),
  })

  const actualizar = (cambios: Record<string, string | string[] | null>) => {
    const nuevos = new URLSearchParams(parametros)
    Object.entries(cambios).forEach(([clave, valor]) => {
      nuevos.delete(clave)
      if (Array.isArray(valor)) valor.forEach((v) => nuevos.append(clave, v))
      else if (valor !== null && valor !== '') nuevos.set(clave, valor)
    })
    if (!('pagina' in cambios)) nuevos.delete('pagina')
    setParametros(nuevos)
  }

  const buscar = () => actualizar({
    entrada: borrador.entrada, salida: borrador.salida, adultos: String(borrador.adultos),
    menores: String(borrador.menores), mascotas: String(borrador.mascotas),
  })

  const alternarCaracteristica = (c: string) => actualizar({
    caracteristica: caracteristicas.includes(c) ? caracteristicas.filter((x) => x !== c) : [...caracteristicas, c],
  })

  const abrir = (codigo: string) =>
    navegar(`/apartamentos/${codigo}?entrada=${entrada}&salida=${salida}&adultos=${adultos}&menores=${menores}&mascotas=${mascotas}`)

  return (
    <>
      <Box sx={{ bgcolor: colores.cafetal, color: colores.papel, pt: { xs: 5, md: 8 }, pb: { xs: 10, md: 12 } }}>
        <Container maxWidth="lg">
          <Typography variant="overline" sx={{ color: colores.guadua }}>Paisaje Cultural Cafetero</Typography>
          <Typography variant="h1" sx={{ maxWidth: 720, mt: 1 }}>
            Siete apartamentos de bahareque a tres cuadras del parque de Filandia
          </Typography>
          <Typography sx={{ mt: 2, maxWidth: 620, opacity: 0.85, fontSize: '1.1rem' }}>
            Cocina propia, vista al valle y el ritmo tranquilo del pueblo. Reserve el apartamento completo para su grupo.
          </Typography>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ mt: { xs: -7, md: -8 } }}>
        <Card sx={{ p: { xs: 2, md: 3 } }}>
          <Grid container spacing={2} sx={{ alignItems: 'center' }}>
            <Grid size={{ xs: 6, md: 2.4 }}>
              <CampoFecha label="Llegada" value={borrador.entrada} slotProps={{ htmlInput: { min: hoyIso() } }}
                          onChange={(e) => setBorrador({ ...borrador, entrada: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 6, md: 2.4 }}>
              <CampoFecha label="Salida" value={borrador.salida} slotProps={{ htmlInput: { min: borrador.entrada } }}
                          onChange={(e) => setBorrador({ ...borrador, salida: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 4, md: 1.6 }}>
              <TextField select label="Adultos" value={borrador.adultos}
                         onChange={(e) => setBorrador({ ...borrador, adultos: Number(e.target.value) })}>
                {Array.from({ length: 8 }, (_, i) => i + 1).map((n) => <MenuItem key={n} value={n}>{n}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 4, md: 2 }}>
              <TextField select label={umbral !== undefined ? `Menores de ${umbral} años` : 'Menores'} value={borrador.menores}
                         onChange={(e) => setBorrador({ ...borrador, menores: Number(e.target.value) })}>
                {Array.from({ length: 6 }, (_, i) => i).map((n) => <MenuItem key={n} value={n}>{n}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 4, md: 1.4 }}>
              <TextField select label="Mascotas" value={borrador.mascotas}
                         onChange={(e) => setBorrador({ ...borrador, mascotas: Number(e.target.value) })}>
                {[0, 1, 2].map((n) => <MenuItem key={n} value={n}>{n}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, md: 2.2 }}>
              <Button fullWidth variant="contained" color="secondary" size="large" startIcon={<BuscarIcon />} onClick={buscar}>
                Buscar
              </Button>
            </Grid>
          </Grid>
          {umbral !== undefined && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1.5 }}>
              Se cobra por persona y por noche. Los menores de {umbral} años ocupan cupo pero no pagan.
            </Typography>
          )}
        </Card>
      </Container>

      <Container maxWidth="lg" sx={{ mt: 5 }}>
        <Grid container spacing={4}>
          <Grid size={{ xs: 12, md: 3 }}>
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ mb: 2 }}>Filtros</Typography>
                <Stack spacing={2}>
                  <TextField select label="Capacidad mínima" value={capacidadMinima ?? ''}
                             onChange={(e) => actualizar({ capacidad: e.target.value })}>
                    <MenuItem value="">Cualquiera</MenuItem>
                    {[2, 3, 4, 5, 6, 8].map((n) => <MenuItem key={n} value={String(n)}>{n} personas</MenuItem>)}
                  </TextField>
                  <TextField label="Precio máximo del grupo" type="number" value={precioMaximo ?? ''}
                             onChange={(e) => actualizar({ precioMax: e.target.value })}
                             slotProps={{ input: { startAdornment: <InputAdornment position="start">$</InputAdornment> } }} />
                  <Box>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Características</Typography>
                    <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 0.75 }}>
                      {todasLasCaracteristicas.map((c) => (
                        <Chip key={c} label={etiqueta(c)} size="small" onClick={() => alternarCaracteristica(c)}
                              color={caracteristicas.includes(c) ? 'primary' : 'default'}
                              variant={caracteristicas.includes(c) ? 'filled' : 'outlined'} />
                      ))}
                    </Stack>
                  </Box>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12, md: 9 }}>
            {resultados.isLoading && <Cargando texto="Buscando apartamentos disponibles…" />}
            {resultados.isError && <MensajeError error={resultados.error} reintentar={() => resultados.refetch()} />}
            {resultados.data && (
              <>
                <Typography color="text.secondary" sx={{ mb: 2 }}>
                  {resultados.data.totalElementos === 1 ? '1 apartamento disponible' : `${resultados.data.totalElementos} apartamentos disponibles`}
                </Typography>
                {resultados.data.contenido.length === 0 ? (
                  <Vacio texto="No hay apartamentos disponibles para esas fechas y ese grupo. Pruebe otras fechas o quite filtros." />
                ) : (
                  <Grid container spacing={3}>
                    {resultados.data.contenido.map((t) => (
                      <Grid key={t.codigo} size={{ xs: 12, sm: 6, lg: 4 }}>
                        <TarjetaApartamento tarjeta={t} alAbrir={() => abrir(t.codigo)} />
                      </Grid>
                    ))}
                  </Grid>
                )}
                <Paginador pagina={resultados.data.pagina} totalPaginas={resultados.data.totalPaginas}
                           alCambiar={(p) => actualizar({ pagina: String(p) })} />
              </>
            )}
          </Grid>
        </Grid>
      </Container>
    </>
  )
}

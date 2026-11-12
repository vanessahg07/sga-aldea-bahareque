import { useMemo } from 'react'
import {
  Box, Button, Card, CardActionArea, CardContent, CardMedia, Chip, IconButton, Link, Stack, Table, TableBody,
  TableCell, TableHead, TableRow, TextField, Tooltip, Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/PersonAddAlt1Outlined'
import QuitarIcon from '@mui/icons-material/CloseRounded'
import LugarIcon from '@mui/icons-material/PlaceOutlined'
import PersonasIcon from '@mui/icons-material/PeopleAltOutlined'
import CamaIcon from '@mui/icons-material/BedOutlined'
import type { Cotizacion, Noche, Ocupante, Tarjeta } from '../api/tipos'
import { colores } from '../tema/tema'
import { aFecha, etiqueta, formatoDia, formatoPesos, isoLocal } from '../utilidades/formato'
import { CampoFecha } from './Comunes'

/** Desglose noche por noche: fecha, temporada, tarifa, facturables y subtotal. */
export function DesgloseCotizacion({ cotizacion, compacto }: { cotizacion: Cotizacion; compacto?: boolean }) {
  return (
    <Box>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Noche</TableCell>
            {!compacto && <TableCell>Temporada</TableCell>}
            <TableCell align="right">Tarifa</TableCell>
            <TableCell align="center">Facturables</TableCell>
            <TableCell align="right">Subtotal</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {cotizacion.detalle.map((n) => (
            <TableRow key={n.fecha}>
              <TableCell>{formatoDia(n.fecha)}</TableCell>
              {!compacto && <TableCell>{etiqueta(n.temporada)}</TableCell>}
              <TableCell align="right">{formatoPesos(n.tarifa)}</TableCell>
              <TableCell align="center">{n.ocupantesFacturables}</TableCell>
              <TableCell align="right">{formatoPesos(n.subtotal)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <Stack spacing={0.5} sx={{ mt: 1.5, px: 2 }}>
        {cotizacion.descuento > 0 && (
          <Fila etiqueta="Descuento por estancia larga" valor={`− ${formatoPesos(cotizacion.descuento)}`} />
        )}
        <Fila etiqueta="Alojamiento" valor={formatoPesos(cotizacion.valorAlojamiento)} />
        {cotizacion.valorMascotas > 0 && <Fila etiqueta="Mascotas" valor={formatoPesos(cotizacion.valorMascotas)} />}
        <Fila etiqueta="Total" valor={formatoPesos(cotizacion.valorTotal)} fuerte />
      </Stack>
    </Box>
  )
}

function Fila({ etiqueta, valor, fuerte }: { etiqueta: string; valor: string; fuerte?: boolean }) {
  return (
    <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
      <Typography color={fuerte ? 'text.primary' : 'text.secondary'} sx={{ fontWeight: fuerte ? 600 : 400 }}>{etiqueta}</Typography>
      <Typography sx={{ fontWeight: fuerte ? 600 : 400, fontSize: fuerte ? '1.1rem' : undefined }}>{valor}</Typography>
    </Stack>
  )
}

/** Lista editable de acompañantes con su fecha de nacimiento. */
export function EditorOcupantes({ ocupantes, alCambiar, maximo }: {
  ocupantes: Ocupante[]
  alCambiar: (ocupantes: Ocupante[]) => void
  maximo?: number
}) {
  const actualizar = (i: number, cambio: Partial<Ocupante>) =>
    alCambiar(ocupantes.map((o, j) => (j === i ? { ...o, ...cambio } : o)))
  return (
    <Stack spacing={1.5}>
      {ocupantes.map((o, i) => (
        <Stack key={i} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <TextField label={`Acompañante ${i + 1}`} value={o.nombre} onChange={(e) => actualizar(i, { nombre: e.target.value })} />
          <CampoFecha label="Fecha de nacimiento" value={o.fechaNacimiento}
                      onChange={(e) => actualizar(i, { fechaNacimiento: e.target.value })} sx={{ maxWidth: 190 }} />
          <IconButton aria-label="Quitar acompañante" onClick={() => alCambiar(ocupantes.filter((_, j) => j !== i))}>
            <QuitarIcon />
          </IconButton>
        </Stack>
      ))}
      <Box>
        <Button startIcon={<AddIcon />} size="small" disabled={maximo !== undefined && ocupantes.length >= maximo}
                onClick={() => alCambiar([...ocupantes, { nombre: '', fechaNacimiento: '' }])}>
          Agregar acompañante
        </Button>
      </Box>
    </Stack>
  )
}

const mayuscula = (texto: string) => texto.charAt(0).toUpperCase() + texto.slice(1)

const COLOR_NOCHE: Record<Noche['estado'], { fondo: string; color: string; texto: string }> = {
  DISPONIBLE: { fondo: colores.cafetalClaro, color: colores.cafetalOscuro, texto: 'Disponible' },
  RESERVADA: { fondo: colores.tejaClara, color: colores.tejaOscura, texto: 'Reservada' },
  BLOQUEADA: { fondo: '#E6E0D6', color: '#6B5E52', texto: 'Bloqueada' },
  NO_A_LA_VENTA: { fondo: '#E6E0D6', color: '#6B5E52', texto: 'No disponible' },
}

/** Calendario mensual de disponibilidad, noche por noche. */
export function CalendarioDisponibilidad({ noches }: { noches: Noche[] }) {
  const meses = useMemo(() => {
    const porMes = new Map<string, Noche[]>()
    noches.forEach((n) => {
      const clave = n.fecha.slice(0, 7)
      porMes.set(clave, [...(porMes.get(clave) ?? []), n])
    })
    return [...porMes.entries()]
  }, [noches])
  const hoy = isoLocal(new Date())

  return (
    <Box>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 3 }}>
        {meses.map(([clave, lista]) => {
          const primero = aFecha(`${clave}-01`)
          const desfase = (primero.getDay() + 6) % 7
          const porFecha = new Map(lista.map((n) => [n.fecha, n]))
          const diasMes = new Date(primero.getFullYear(), primero.getMonth() + 1, 0).getDate()
          return (
            <Box key={clave}>
              <Typography variant="h6" sx={{ mb: 1 }}>
                {mayuscula(primero.toLocaleDateString('es-CO', { month: 'long', year: 'numeric' }))}
              </Typography>
              <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 0.5 }}>
                {['L', 'M', 'M', 'J', 'V', 'S', 'D'].map((d, i) => (
                  <Typography key={i} variant="caption" color="text.secondary" sx={{ textAlign: 'center' }}>{d}</Typography>
                ))}
                {Array.from({ length: desfase }).map((_, i) => <Box key={`v${i}`} />)}
                {Array.from({ length: diasMes }).map((_, i) => {
                  const fecha = `${clave}-${String(i + 1).padStart(2, '0')}`
                  const noche = porFecha.get(fecha)
                  const estilo = noche ? COLOR_NOCHE[noche.estado] : null
                  return (
                    <Tooltip key={fecha} title={estilo ? `${formatoDia(fecha)}: ${estilo.texto}` : ''}>
                      <Box sx={{
                        textAlign: 'center', py: 0.6, borderRadius: 1, fontSize: 13,
                        bgcolor: estilo?.fondo ?? 'transparent', color: estilo?.color ?? 'text.disabled',
                        outline: fecha === hoy ? `2px solid ${colores.teja}` : 'none',
                      }}>
                        {i + 1}
                      </Box>
                    </Tooltip>
                  )
                })}
              </Box>
            </Box>
          )
        })}
      </Box>
      <Stack direction="row" spacing={2} sx={{ mt: 2, flexWrap: 'wrap' }}>
        {(['DISPONIBLE', 'RESERVADA', 'BLOQUEADA'] as const).map((e) => (
          <Stack key={e} direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
            <Box sx={{ width: 14, height: 14, borderRadius: 0.5, bgcolor: COLOR_NOCHE[e].fondo }} />
            <Typography variant="caption">{COLOR_NOCHE[e].texto}</Typography>
          </Stack>
        ))}
      </Stack>
    </Box>
  )
}

/**
 * Ubicación del alojamiento. Con VITE_MAPBOX_TOKEN muestra un mapa estático de Mapbox;
 * sin él (o si el mapa falla), muestra la dirección y un enlace a OpenStreetMap.
 */
export function MapaUbicacion({ latitud, longitud, direccion, ciudad }: {
  latitud: number
  longitud: number
  direccion?: string | null
  ciudad: string
}) {
  const token = import.meta.env.VITE_MAPBOX_TOKEN as string | undefined
  const enlace = `https://www.openstreetmap.org/?mlat=${latitud}&mlon=${longitud}#map=16/${latitud}/${longitud}`
  const imagen = token
    ? `https://api.mapbox.com/styles/v1/mapbox/outdoors-v12/static/pin-l+b4532f(${longitud},${latitud})/${longitud},${latitud},14,0/640x320@2x?access_token=${token}`
    : null
  return (
    <Card>
      {imagen && <CardMedia component="img" image={imagen} alt={`Mapa de ${ciudad}`} sx={{ height: 220 }}
                            onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }} />}
      <CardContent>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
          <LugarIcon color="secondary" />
          <Box>
            <Typography sx={{ fontWeight: 600 }}>{direccion ?? ciudad}</Typography>
            <Typography variant="body2" color="text.secondary">{ciudad} · {latitud.toFixed(4)}, {longitud.toFixed(4)}</Typography>
            <Link href={enlace} target="_blank" rel="noopener" variant="body2">Ver en el mapa</Link>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

/** Tarjeta de resultado de búsqueda. */
export function TarjetaApartamento({ tarjeta, alAbrir }: { tarjeta: Tarjeta; alAbrir: () => void }) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardActionArea onClick={alAbrir} sx={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'stretch' }}>
        <CardMedia component="img" image={tarjeta.imagenPrincipal ?? undefined} alt={tarjeta.nombre}
                   sx={{ height: 180, bgcolor: colores.borde }} />
        <CardContent sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', gap: 1 }}>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'baseline' }}>
            <Typography variant="h5">{tarjeta.nombre}</Typography>
            <Typography variant="caption" color="text.secondary">{tarjeta.codigo}</Typography>
          </Stack>
          <Stack direction="row" spacing={2} sx={{ color: 'text.secondary' }}>
            <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}><PersonasIcon fontSize="small" /><Typography variant="body2">Hasta {tarjeta.capacidad}</Typography></Stack>
            <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}><CamaIcon fontSize="small" /><Typography variant="body2">{tarjeta.dormitorios} dorm.</Typography></Stack>
          </Stack>
          <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 0.5 }}>
            {tarjeta.caracteristicas.slice(0, 3).map((c) => <Chip key={c} label={etiqueta(c)} size="small" variant="outlined" />)}
          </Stack>
          <Box sx={{ mt: 'auto', pt: 1 }}>
            <Typography variant="h5" color="secondary">{formatoPesos(tarjeta.valorEstimado)}</Typography>
            <Typography variant="caption" color="text.secondary">por {tarjeta.noches} {tarjeta.noches === 1 ? 'noche' : 'noches'} para el grupo</Typography>
          </Box>
        </CardContent>
      </CardActionArea>
    </Card>
  )
}

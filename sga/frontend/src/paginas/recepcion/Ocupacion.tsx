import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useQueries, useQuery } from '@tanstack/react-query'
import { Box, Card, Container, Stack, Tooltip, Typography } from '@mui/material'
import { apartamentos, reservas } from '../../api/servicios'
import { Cargando, CampoFecha, Encabezado, MensajeError } from '../../componentes/Comunes'
import { colores } from '../../tema/tema'
import { estiloReserva } from '../../utilidades/estados'
import { aFecha, formatoFecha, hoyIso, sumarDias } from '../../utilidades/formato'

const DIAS = 21

/** Calendario de ocupación: apartamentos en filas, noches en columnas. */
export function Ocupacion() {
  const navegar = useNavigate()
  const [desde, setDesde] = useState(hoyIso())
  const hasta = sumarDias(desde, DIAS)
  const inventario = useQuery({ queryKey: ['apartamentos', 'gestion', 0], queryFn: () => apartamentos.gestion(0) })
  const ocupacion = useQuery({ queryKey: ['reservas', 'ocupacion', desde], queryFn: () => reservas.ocupacion(desde, hasta) })
  const codigos = inventario.data?.contenido.map((a) => a.codigo) ?? []
  const bloqueos = useQueries({
    queries: codigos.map((c) => ({ queryKey: ['bloqueos', c], queryFn: () => apartamentos.bloqueos(c) })),
  })
  const dias = Array.from({ length: DIAS }, (_, i) => sumarDias(desde, i))

  if (inventario.isLoading || ocupacion.isLoading) return <Cargando />
  if (inventario.isError) return <Container sx={{ py: 4 }}><MensajeError error={inventario.error} /></Container>
  if (ocupacion.isError) return <Container sx={{ py: 4 }}><MensajeError error={ocupacion.error} /></Container>

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Encabezado titulo="Ocupación" subtitulo={`${formatoFecha(desde)} a ${formatoFecha(sumarDias(hasta, -1))}`}
                  acciones={<CampoFecha label="Desde" value={desde} onChange={(e) => setDesde(e.target.value || hoyIso())} sx={{ width: 170 }} />} />
      <Card sx={{ overflowX: 'auto' }}>
        <Box sx={{ display: 'grid', gridTemplateColumns: `130px repeat(${DIAS}, minmax(38px, 1fr))`, minWidth: 130 + DIAS * 38 }}>
          <Box sx={{ p: 1, borderBottom: 1, borderColor: 'divider' }} />
          {dias.map((d) => {
            const fecha = aFecha(d)
            const finDeSemana = fecha.getDay() === 0 || fecha.getDay() === 6
            return (
              <Box key={d} sx={{ p: 0.5, textAlign: 'center', borderBottom: 1, borderColor: 'divider', bgcolor: finDeSemana ? colores.cal : undefined }}>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', lineHeight: 1.2 }}>
                  {fecha.toLocaleDateString('es-CO', { weekday: 'narrow' })}
                </Typography>
                <Typography variant="body2" sx={{ fontWeight: d === hoyIso() ? 700 : 400, color: d === hoyIso() ? colores.teja : undefined }}>
                  {fecha.getDate()}
                </Typography>
              </Box>
            )
          })}
          {inventario.data!.contenido.map((a, fila) => (
            <Box key={a.codigo} sx={{ display: 'contents' }}>
              <Box sx={{ p: 1, borderBottom: 1, borderColor: 'divider' }}>
                <Typography sx={{ fontWeight: 600 }}>{a.codigo}</Typography>
                <Typography variant="caption" color="text.secondary">{a.nombre}</Typography>
              </Box>
              {dias.map((d) => {
                const reserva = ocupacion.data!.find((r) => r.codigoApartamento === a.codigo && r.entrada <= d && d < r.salida)
                const bloqueo = bloqueos[fila]?.data?.find((b) => b.desde <= d && d < b.hasta)
                const estilo = reserva ? estiloReserva[reserva.estado] : null
                const inicio = reserva && (reserva.entrada === d || d === desde)
                const primera = reserva ? reserva.entrada === d : bloqueo?.desde === d || d === desde
                const ultima = reserva ? sumarDias(d, 1) === reserva.salida : sumarDias(d, 1) === bloqueo?.hasta
                return (
                  <Tooltip key={d} title={reserva ? `${reserva.titular} · ${estiloReserva[reserva.estado].texto} · ${formatoFecha(reserva.entrada)} → ${formatoFecha(reserva.salida)}`
                    : bloqueo ? `Bloqueado: ${bloqueo.motivo}` : ''}>
                    <Box onClick={() => reserva && navegar(`/reservas/${reserva.id}`)}
                         sx={{ borderBottom: 1, borderColor: 'divider', py: 0.5, pl: primera ? 0.5 : 0, pr: ultima ? 0.5 : 0,
                               cursor: reserva ? 'pointer' : 'default' }}>
                      <Box sx={{
                        height: 34, px: 0.5, overflow: 'visible', whiteSpace: 'nowrap', position: 'relative', zIndex: inicio ? 1 : 0,
                        borderTopLeftRadius: primera ? 6 : 0, borderBottomLeftRadius: primera ? 6 : 0,
                        borderTopRightRadius: ultima ? 6 : 0, borderBottomRightRadius: ultima ? 6 : 0,
                        bgcolor: estilo?.fondo ?? (bloqueo ? '#E6E0D6' : 'transparent'),
                        color: estilo?.color, fontSize: 12, fontWeight: 600, display: 'flex', alignItems: 'center',
                        backgroundImage: bloqueo && !reserva ? `repeating-linear-gradient(45deg, transparent, transparent 4px, ${colores.borde} 4px, ${colores.borde} 8px)` : undefined,
                      }}>
                        {inicio ? reserva!.titular.split(' ')[0] : ''}
                      </Box>
                    </Box>
                  </Tooltip>
                )
              })}
            </Box>
          ))}
        </Box>
      </Card>
      <Stack direction="row" sx={{ mt: 2, flexWrap: 'wrap', gap: 2 }}>
        {(['PENDIENTE', 'CONFIRMADA', 'EN_CURSO'] as const).map((e) => (
          <Stack key={e} direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
            <Box sx={{ width: 14, height: 14, borderRadius: 0.5, bgcolor: estiloReserva[e].fondo }} />
            <Typography variant="caption">{estiloReserva[e].texto}</Typography>
          </Stack>
        ))}
        <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
          <Box sx={{ width: 14, height: 14, borderRadius: 0.5, bgcolor: '#E6E0D6' }} />
          <Typography variant="caption">Bloqueado</Typography>
        </Stack>
      </Stack>
    </Container>
  )
}

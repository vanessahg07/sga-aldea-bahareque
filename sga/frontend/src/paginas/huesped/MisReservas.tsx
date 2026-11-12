import { useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { Box, Button, Card, CardActionArea, CardContent, Container, Stack, Typography } from '@mui/material'
import { reservas } from '../../api/servicios'
import { Cargando, Encabezado, EstadoReservaChip, MensajeError, Paginador, Vacio } from '../../componentes/Comunes'
import { formatoFecha, formatoPesos, noches } from '../../utilidades/formato'

export function MisReservas() {
  const navegar = useNavigate()
  const [pagina, setPagina] = useState(0)
  const consulta = useQuery({ queryKey: ['reservas', 'mias', pagina], queryFn: () => reservas.mias(pagina) })

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Encabezado titulo="Mis reservas" subtitulo="De la más reciente a la más antigua."
                  acciones={<Button variant="contained" color="secondary" component={RouterLink} to="/">Nueva reserva</Button>} />
      {consulta.isLoading && <Cargando />}
      {consulta.isError && <MensajeError error={consulta.error} reintentar={() => consulta.refetch()} />}
      {consulta.data && (
        consulta.data.contenido.length === 0 ? <Vacio texto="Aún no tiene reservas." /> : (
          <Stack spacing={2}>
            {consulta.data.contenido.map((r) => (
              <Card key={r.id}>
                <CardActionArea onClick={() => navegar(`/reservas/${r.id}`)}>
                  <CardContent>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ justifyContent: 'space-between' }}>
                      <Box>
                        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                          <Typography variant="h5">{r.codigoApartamento}</Typography>
                          <EstadoReservaChip estado={r.estado} pequeno />
                        </Stack>
                        <Typography color="text.secondary">
                          {formatoFecha(r.entrada)} → {formatoFecha(r.salida)} · {noches(r.entrada, r.salida)} noches · {r.totalOcupantes} personas
                        </Typography>
                      </Box>
                      <Box sx={{ textAlign: { sm: 'right' } }}>
                        <Typography variant="h5">{formatoPesos(r.valorTotal)}</Typography>
                        <Typography variant="body2" color={r.saldo > 0 ? 'secondary' : 'text.secondary'}>
                          {r.saldo > 0 ? `Saldo pendiente ${formatoPesos(r.saldo)}` : r.saldo < 0 ? `Saldo a favor ${formatoPesos(-r.saldo)}` : 'A paz y salvo'}
                        </Typography>
                      </Box>
                    </Stack>
                  </CardContent>
                </CardActionArea>
              </Card>
            ))}
          </Stack>
        )
      )}
      {consulta.data && <Paginador pagina={consulta.data.pagina} totalPaginas={consulta.data.totalPaginas} alCambiar={setPagina} />}
    </Container>
  )
}

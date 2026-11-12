import { useState } from 'react'
import { Link as RouterLink } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Button, Card, Chip, Container, Link, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
} from '@mui/material'
import { alojamiento, recordatorios } from '../../api/servicios'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, Encabezado, MensajeError, Paginador, Vacio } from '../../componentes/Comunes'
import { formatoFechaHora } from '../../utilidades/formato'
import { colores } from '../../tema/tema'

/**
 * Recordatorios automáticos (funcionalidad opcional): se envían cada día a las 8:00 y
 * también se pueden enviar a mano. Cada reserva recibe cada recordatorio una sola vez.
 */
export function AdminRecordatorios() {
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const [pagina, setPagina] = useState(0)
  const aloj = useQuery({ queryKey: ['alojamiento'], queryFn: alojamiento.consultar })
  const consulta = useQuery({ queryKey: ['recordatorios', pagina], queryFn: () => recordatorios.listar(pagina) })
  const enviar = useMutation({
    mutationFn: recordatorios.enviarAhora,
    onSuccess: (enviados) => {
      avisos.exito(enviados.length === 0 ? 'No había recordatorios pendientes para hoy' : `Recordatorios enviados: ${enviados.length}`)
      clienteConsultas.invalidateQueries({ queryKey: ['recordatorios'] })
    },
    onError: avisos.error,
  })
  const dias = aloj.data?.parametros.diasRecordatorioLlegada

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Encabezado titulo="Recordatorios"
                  subtitulo="Avisos automáticos al titular antes de la llegada y el día anterior a la salida."
                  acciones={<Button variant="contained" color="secondary" loading={enviar.isPending} onClick={() => enviar.mutate()}>
                    Enviar los de hoy
                  </Button>} />
      {dias !== undefined && (
        <Alert severity="info" sx={{ mb: 3 }}>
          {dias > 0
            ? <>La llegada se recuerda {dias === 1 ? 'un día' : `${dias} días`} antes a las reservas confirmadas; la salida, el día anterior. </>
            : <>El recordatorio de llegada está desactivado; solo se recuerda la salida. </>}
          Se cambia en <Link component={RouterLink} to="/admin/alojamiento">los parámetros del alojamiento</Link>.
        </Alert>
      )}
      {consulta.isLoading && <Cargando />}
      {consulta.isError && <MensajeError error={consulta.error} />}
      {consulta.data && consulta.data.contenido.length === 0 && <Vacio texto="Aún no se han enviado recordatorios." />}
      {consulta.data && consulta.data.contenido.length > 0 && (
        <Card>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow><TableCell>Enviado</TableCell><TableCell>Tipo</TableCell><TableCell>Destinatario</TableCell><TableCell>Asunto</TableCell><TableCell /></TableRow>
              </TableHead>
              <TableBody>
                {consulta.data.contenido.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFechaHora(r.enviadoEn)}</TableCell>
                    <TableCell>
                      <Chip size="small" label={r.tipo === 'LLEGADA' ? 'Llegada' : 'Salida'}
                            sx={r.tipo === 'LLEGADA' ? { bgcolor: colores.cafetalClaro, color: colores.cafetalOscuro } : { bgcolor: colores.niebla, color: colores.nieblaOscura }} />
                    </TableCell>
                    <TableCell>{r.destinatario}</TableCell>
                    <TableCell>{r.asunto}</TableCell>
                    <TableCell align="right"><Link component={RouterLink} to={`/reservas/${r.idReserva}`}>Reserva</Link></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      )}
      {consulta.data && <Paginador pagina={consulta.data.pagina} totalPaginas={consulta.data.totalPaginas} alCambiar={setPagina} />}
    </Container>
  )
}

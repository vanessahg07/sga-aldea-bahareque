import { useQuery } from '@tanstack/react-query'
import { Card, CardContent, Container, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material'
import { tarifas } from '../../api/servicios'
import { Cargando, Encabezado, MensajeError } from '../../componentes/Comunes'
import { formatoFecha } from '../../utilidades/formato'

/** Política de cancelación vigente, pública para que el huésped la conozca antes de reservar. */
export function PoliticaPublica() {
  const politica = useQuery({ queryKey: ['politica', 'vigente'], queryFn: tarifas.politicaVigente })
  if (politica.isLoading) return <Cargando />
  if (politica.isError) return <Container sx={{ py: 4 }}><MensajeError error={politica.error} /></Container>
  const p = politica.data!
  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Encabezado titulo="Política de cancelación" subtitulo={`Versión ${p.version}, vigente desde ${formatoFecha(p.vigenteDesde)}`} />
      <Card>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Si cancela con…</TableCell>
              <TableCell align="right">Se retiene del valor de la estancia</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {p.tramos.map((t, i) => (
              <TableRow key={t.diasAntelacionMinimos}>
                <TableCell>
                  {i === 0 ? `${t.diasAntelacionMinimos} días o más de antelación`
                    : t.diasAntelacionMinimos === 0 ? `Menos de ${p.tramos[i - 1].diasAntelacionMinimos} días`
                      : `De ${t.diasAntelacionMinimos} a ${p.tramos[i - 1].diasAntelacionMinimos - 1} días`}
                </TableCell>
                <TableCell align="right">{t.porcentajeRetencion}%</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <CardContent>
          <Typography color="text.secondary">
            Si no se presenta el día de llegada se retiene el {p.porcentajeRetencionNoShow}% del valor de la estancia.
            Lo pagado de más queda como saldo a favor. Cada reserva conserva la versión de la política vigente cuando se creó.
          </Typography>
        </CardContent>
      </Card>
    </Container>
  )
}

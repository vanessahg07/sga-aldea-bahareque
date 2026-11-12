import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import {
  Button, Card, CardContent, Container, Grid, MenuItem, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TextField, Typography,
} from '@mui/material'
import { reservas } from '../../api/servicios'
import type { Canal, EstadoReserva } from '../../api/tipos'
import { Cargando, CampoFecha, Encabezado, EstadoReservaChip, MensajeError, Paginador, Vacio } from '../../componentes/Comunes'
import { CANALES, ESTADOS_RESERVA, estiloReserva, nombreCanal } from '../../utilidades/estados'
import { formatoFecha, formatoFechaHora, formatoPesos } from '../../utilidades/formato'

export function ListadoReservas() {
  const navegar = useNavigate()
  const [parametros, setParametros] = useSearchParams()
  const filtro = {
    desde: parametros.get('desde') ?? '',
    hasta: parametros.get('hasta') ?? '',
    estado: (parametros.get('estado') ?? '') as EstadoReserva | '',
    apartamento: parametros.get('apartamento') ?? '',
    canal: (parametros.get('canal') ?? '') as Canal | '',
    titular: parametros.get('titular') ?? '',
    pagina: Number(parametros.get('pagina') ?? 0),
  }
  const consulta = useQuery({ queryKey: ['reservas', 'listado', filtro], queryFn: () => reservas.listar(filtro) })

  const cambiar = (clave: string, valor: string) => {
    const nuevos = new URLSearchParams(parametros)
    if (valor) nuevos.set(clave, valor)
    else nuevos.delete(clave)
    if (clave !== 'pagina') nuevos.delete('pagina')
    setParametros(nuevos)
  }

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Encabezado titulo="Reservas" subtitulo="De la más reciente a la más antigua."
                  acciones={<Button variant="contained" color="secondary" component={RouterLink} to="/recepcion/nueva">Reserva directa</Button>} />
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid size={{ xs: 6, md: 2 }}><CampoFecha label="Estancia desde" value={filtro.desde} onChange={(e) => cambiar('desde', e.target.value)} /></Grid>
            <Grid size={{ xs: 6, md: 2 }}><CampoFecha label="Estancia hasta" value={filtro.hasta} onChange={(e) => cambiar('hasta', e.target.value)} /></Grid>
            <Grid size={{ xs: 6, md: 2 }}>
              <TextField select label="Estado" value={filtro.estado} onChange={(e) => cambiar('estado', e.target.value)}>
                <MenuItem value="">Todos</MenuItem>
                {ESTADOS_RESERVA.map((e) => <MenuItem key={e} value={e}>{estiloReserva[e].texto}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 6, md: 2 }}>
              <TextField select label="Canal" value={filtro.canal} onChange={(e) => cambiar('canal', e.target.value)}>
                <MenuItem value="">Todos</MenuItem>
                {CANALES.map((c) => <MenuItem key={c} value={c}>{nombreCanal[c]}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 6, md: 2 }}>
              <TextField label="Apartamento" placeholder="BH-201" value={filtro.apartamento} onChange={(e) => cambiar('apartamento', e.target.value)} />
            </Grid>
            <Grid size={{ xs: 6, md: 2 }}>
              <TextField label="Titular" placeholder="Nombre o documento" value={filtro.titular} onChange={(e) => cambiar('titular', e.target.value)} />
            </Grid>
          </Grid>
        </CardContent>
      </Card>
      {consulta.isLoading && <Cargando />}
      {consulta.isError && <MensajeError error={consulta.error} />}
      {consulta.data && (consulta.data.contenido.length === 0 ? <Vacio texto="No hay reservas con esos filtros." /> : (
        <Card>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Apartamento</TableCell><TableCell>Titular</TableCell><TableCell>Entrada</TableCell>
                  <TableCell>Salida</TableCell><TableCell align="center">Personas</TableCell><TableCell>Canal</TableCell>
                  <TableCell>Estado</TableCell><TableCell align="right">Valor</TableCell><TableCell align="right">Saldo</TableCell>
                  <TableCell>Creada</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {consulta.data.contenido.map((r) => (
                  <TableRow key={r.id} hover sx={{ cursor: 'pointer' }} onClick={() => navegar(`/reservas/${r.id}`)}>
                    <TableCell sx={{ fontWeight: 600 }}>{r.codigoApartamento}</TableCell>
                    <TableCell>{r.titular}</TableCell>
                    <TableCell>{formatoFecha(r.entrada)}</TableCell>
                    <TableCell>{formatoFecha(r.salida)}</TableCell>
                    <TableCell align="center">{r.totalOcupantes}</TableCell>
                    <TableCell>{nombreCanal[r.canal]}</TableCell>
                    <TableCell><EstadoReservaChip estado={r.estado} pequeno /></TableCell>
                    <TableCell align="right">{formatoPesos(r.valorTotal)}</TableCell>
                    <TableCell align="right">
                      <Typography variant="body2" color={r.saldo > 0 ? 'secondary' : 'text.primary'}>{formatoPesos(r.saldo)}</Typography>
                    </TableCell>
                    <TableCell>{formatoFechaHora(r.creadaEn)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      ))}
      {consulta.data && (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>{consulta.data.totalElementos} reservas</Typography>
          <Paginador pagina={consulta.data.pagina} totalPaginas={consulta.data.totalPaginas} alCambiar={(p) => cambiar('pagina', String(p))} />
        </>
      )}
    </Container>
  )
}

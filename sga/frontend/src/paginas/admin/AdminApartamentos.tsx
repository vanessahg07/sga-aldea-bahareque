import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Box, Button, Card, Chip, Container, Dialog, DialogActions, DialogContent, DialogTitle, Stack, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow,
} from '@mui/material'
import { apartamentos } from '../../api/servicios'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, Encabezado, EstadoOperativoChip, MensajeError, Paginador } from '../../componentes/Comunes'
import { colores } from '../../tema/tema'
import { FormularioApartamento, apartamentoVacio, type DatosFormulario } from './FormularioApartamento'

export function AdminApartamentos() {
  const navegar = useNavigate()
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const [pagina, setPagina] = useState(0)
  const [nuevo, setNuevo] = useState<DatosFormulario | null>(null)
  const consulta = useQuery({ queryKey: ['apartamentos', 'gestion', pagina], queryFn: () => apartamentos.gestion(pagina) })

  const crear = useMutation({
    mutationFn: (d: DatosFormulario) => apartamentos.crear(d.codigo, d),
    onSuccess: (a) => {
      avisos.exito(`Apartamento ${a.codigo} creado. Agregue imágenes y tarifas para activarlo.`)
      clienteConsultas.invalidateQueries({ queryKey: ['apartamentos'] })
      navegar(`/admin/apartamentos/${a.codigo}`)
    },
    onError: avisos.error,
  })

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Encabezado titulo="Inventario" subtitulo="Apartamentos del alojamiento, estén o no a la venta."
                  acciones={<Button variant="contained" color="secondary" onClick={() => setNuevo(apartamentoVacio)}>Nuevo apartamento</Button>} />
      {consulta.isLoading && <Cargando />}
      {consulta.isError && <MensajeError error={consulta.error} />}
      {consulta.data && (
        <Card>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell /><TableCell>Código</TableCell><TableCell>Nombre</TableCell><TableCell align="center">Dormitorios</TableCell>
                  <TableCell align="center">Capacidad</TableCell><TableCell>Venta</TableCell><TableCell>Estado operativo</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {consulta.data.contenido.map((a) => (
                  <TableRow key={a.codigo} hover sx={{ cursor: 'pointer' }} onClick={() => navegar(`/admin/apartamentos/${a.codigo}`)}>
                    <TableCell sx={{ width: 72 }}>
                      <Box component="img" src={a.imagenPrincipal ?? undefined} alt=""
                           sx={{ width: 56, height: 40, objectFit: 'cover', borderRadius: 1, bgcolor: colores.borde, display: 'block' }} />
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>{a.codigo}</TableCell>
                    <TableCell>{a.nombre}</TableCell>
                    <TableCell align="center">{a.dormitorios}</TableCell>
                    <TableCell align="center">{a.capacidad}</TableCell>
                    <TableCell>{a.activo ? <Chip size="small" label="A la venta" color="primary" /> : <Chip size="small" label="Inactivo" />}</TableCell>
                    <TableCell><EstadoOperativoChip estado={a.estadoOperativo} pequeno /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      )}
      {consulta.data && <Paginador pagina={consulta.data.pagina} totalPaginas={consulta.data.totalPaginas} alCambiar={setPagina} />}

      <Dialog open={!!nuevo} onClose={() => setNuevo(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Nuevo apartamento</DialogTitle>
        <DialogContent>{nuevo && <Stack sx={{ pt: 1 }}><FormularioApartamento datos={nuevo} alCambiar={setNuevo} conCodigo /></Stack>}</DialogContent>
        <DialogActions>
          <Button color="inherit" onClick={() => setNuevo(null)}>Cancelar</Button>
          <Button variant="contained" loading={crear.isPending} disabled={!nuevo?.codigo.trim() || !nuevo?.nombre.trim()}
                  onClick={() => nuevo && crear.mutate(nuevo)}>Crear</Button>
        </DialogActions>
      </Dialog>
    </Container>
  )
}

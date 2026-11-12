import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Box, Button, Card, CardActions, CardContent, Container, Dialog, DialogContent, DialogTitle, Grid, Stack, Typography,
} from '@mui/material'
import { apartamentos } from '../../api/servicios'
import type { Apartamento, EstadoOperativo } from '../../api/tipos'
import { useAvisos } from '../../componentes/Avisos'
import { Cargando, Encabezado, EstadoOperativoChip, MensajeError, Vacio } from '../../componentes/Comunes'
import { DialogoNovedad, HistorialNovedades } from '../../componentes/Novedades'

/** Tablero del personal de servicio: apartamentos por preparar y registro de novedades. */
export function Preparacion() {
  const avisos = useAvisos()
  const clienteConsultas = useQueryClient()
  const [novedadPara, setNovedadPara] = useState<string | null>(null)
  const [historialDe, setHistorialDe] = useState<string | null>(null)

  const pendientes = useQuery({ queryKey: ['apartamentos', 'pendientes'], queryFn: apartamentos.pendientesPreparacion })
  const todos = useQuery({ queryKey: ['apartamentos', 'venta'], queryFn: () => apartamentos.aLaVenta(0) })

  const cambiar = useMutation({
    mutationFn: ({ codigo, estado }: { codigo: string; estado: EstadoOperativo }) => apartamentos.cambiarEstado(codigo, estado),
    onSuccess: (a) => {
      avisos.exito(a.estadoOperativo === 'PREPARADO' ? `${a.codigo} quedó preparado para recibir huéspedes` : `${a.codigo} en preparación`)
      clienteConsultas.invalidateQueries({ queryKey: ['apartamentos'] })
    },
    onError: avisos.error,
  })

  const tarjeta = (a: Apartamento) => (
    <Card key={a.codigo} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ flexGrow: 1 }}>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="h5">{a.codigo}</Typography>
          <EstadoOperativoChip estado={a.estadoOperativo} pequeno />
        </Stack>
        <Typography color="text.secondary">{a.nombre} · {a.dormitorios} dorm. · {a.capacidad} personas</Typography>
      </CardContent>
      <CardActions sx={{ flexWrap: 'wrap', gap: 1, px: 2, pb: 2 }}>
        {a.estadoOperativo === 'PENDIENTE_PREPARACION' && (
          <Button variant="contained" loading={cambiar.isPending && cambiar.variables?.codigo === a.codigo}
                  onClick={() => cambiar.mutate({ codigo: a.codigo, estado: 'EN_PREPARACION' })}>Iniciar preparación</Button>
        )}
        {a.estadoOperativo === 'EN_PREPARACION' && (
          <Button variant="contained" color="secondary" loading={cambiar.isPending && cambiar.variables?.codigo === a.codigo}
                  onClick={() => cambiar.mutate({ codigo: a.codigo, estado: 'PREPARADO' })}>Marcar preparado</Button>
        )}
        <Button onClick={() => setNovedadPara(a.codigo)}>Novedad</Button>
        <Button onClick={() => setHistorialDe(a.codigo)}>Historial</Button>
      </CardActions>
    </Card>
  )

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Encabezado titulo="Preparación de apartamentos"
                  subtitulo="Tras cada salida el apartamento queda pendiente de preparación. Solo se entrega a un grupo cuando está preparado." />
      <Typography variant="h4" sx={{ mb: 2 }}>Por preparar</Typography>
      {pendientes.isLoading && <Cargando />}
      {pendientes.isError && <MensajeError error={pendientes.error} />}
      {pendientes.data && (pendientes.data.length === 0
        ? <Vacio texto="No hay apartamentos pendientes de preparación." />
        : <Grid container spacing={2}>{pendientes.data.map((a) => <Grid key={a.codigo} size={{ xs: 12, sm: 6, md: 4 }}>{tarjeta(a)}</Grid>)}</Grid>)}

      <Typography variant="h4" sx={{ mt: 5, mb: 2 }}>Todos los apartamentos</Typography>
      {todos.data && (
        <Grid container spacing={2}>
          {todos.data.contenido.filter((a) => !pendientes.data?.some((p) => p.codigo === a.codigo))
            .map((a) => <Grid key={a.codigo} size={{ xs: 12, sm: 6, md: 4 }}>{tarjeta(a)}</Grid>)}
        </Grid>
      )}
      <Box sx={{ mt: 2 }}>
        <Typography variant="body2" color="text.secondary">
          Declarar un apartamento fuera de servicio y registrar bloqueos es una decisión del administrador.
        </Typography>
      </Box>

      <DialogoNovedad codigo={novedadPara} alCerrar={() => setNovedadPara(null)} />
      <Dialog open={!!historialDe} onClose={() => setHistorialDe(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Novedades de {historialDe}</DialogTitle>
        <DialogContent>{historialDe && <HistorialNovedades codigo={historialDe} />}</DialogContent>
      </Dialog>
    </Container>
  )
}

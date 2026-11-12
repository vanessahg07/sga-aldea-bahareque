import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField, Typography,
} from '@mui/material'
import { apartamentos, type DatosEstancia } from '../../api/servicios'
import type { Ocupante, Reserva } from '../../api/tipos'
import { CampoFecha, CampoHora } from '../../componentes/Comunes'
import { EditorOcupantes } from '../../componentes/Negocio'

/**
 * Modificación de fechas, grupo o apartamento de una reserva no iniciada. El dominio
 * revalida todo y registra la diferencia de valor como ajuste en el folio.
 */
interface Props {
  abierto: boolean
  reserva: Reserva
  cargando: boolean
  alCerrar: () => void
  alGuardar: (datos: DatosEstancia) => void
}

export function DialogoModificar(props: Props) {
  return props.abierto ? <FormularioModificar {...props} /> : null
}

function FormularioModificar({ reserva, cargando, alCerrar, alGuardar }: Props) {
  const inventario = useQuery({ queryKey: ['apartamentos', 'venta'], queryFn: () => apartamentos.aLaVenta(0) })
  const [codigo, setCodigo] = useState(reserva.codigoApartamento)
  const [entrada, setEntrada] = useState(reserva.entrada)
  const [salida, setSalida] = useState(reserva.salida)
  const [hora, setHora] = useState(reserva.horaEstimadaLlegada?.slice(0, 5) ?? '')
  const [mascotas, setMascotas] = useState(reserva.mascotas)
  const [acompanantes, setAcompanantes] = useState<Ocupante[]>(reserva.acompanantes)

  const incompleto = acompanantes.some((o) => !o.nombre.trim() || !o.fechaNacimiento)
  return (
    <Dialog open onClose={alCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>Modificar reserva</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField select label="Apartamento" value={codigo} onChange={(e) => setCodigo(e.target.value)}>
            {(inventario.data?.contenido ?? []).map((a) => (
              <MenuItem key={a.codigo} value={a.codigo}>{a.codigo} · {a.nombre} (hasta {a.capacidad})</MenuItem>
            ))}
            {!inventario.data && <MenuItem value={codigo}>{codigo}</MenuItem>}
          </TextField>
          <Stack direction="row" spacing={2}>
            <CampoFecha label="Llegada" value={entrada} onChange={(e) => setEntrada(e.target.value)} />
            <CampoFecha label="Salida" value={salida} onChange={(e) => setSalida(e.target.value)} />
          </Stack>
          <Stack direction="row" spacing={2}>
            <CampoHora label="Hora estimada de llegada" value={hora} onChange={(e) => setHora(e.target.value)} />
            <TextField select label="Mascotas" value={mascotas} onChange={(e) => setMascotas(Number(e.target.value))} sx={{ maxWidth: 140 }}>
              {[0, 1, 2].map((n) => <MenuItem key={n} value={n}>{n}</MenuItem>)}
            </TextField>
          </Stack>
          <Typography variant="body2" color="text.secondary">Titular: {reserva.titular.nombre}. Acompañantes:</Typography>
          <EditorOcupantes ocupantes={acompanantes} alCambiar={setAcompanantes} />
          <Typography variant="caption" color="text.secondary">
            El valor se recalcula con las tarifas vigentes y la diferencia queda como ajuste en el folio.
            La política de cancelación de la reserva no cambia.
          </Typography>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button color="inherit" onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" loading={cargando} disabled={incompleto || !entrada || !salida}
                onClick={() => alGuardar({ codigoApartamento: codigo, entrada, salida, acompanantes, horaEstimadaLlegada: hora || null, mascotas })}>
          Guardar cambios
        </Button>
      </DialogActions>
    </Dialog>
  )
}

import { useState, type ReactNode } from 'react'
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, InputAdornment, MenuItem, Stack, TextField,
  Typography,
} from '@mui/material'
import type { Alojamiento } from '../../api/tipos'
import { formatoPesos } from '../../utilidades/formato'

/**
 * Los diálogos solo se montan mientras están abiertos: cada apertura empieza con el
 * estado inicial, sin efectos que lo reinicien.
 */
interface Base {
  abierto: boolean
  cargando: boolean
  alCerrar: () => void
}

function Marco({ titulo, alCerrar, alAceptar, cargando, deshabilitado, textoAceptar, children }: Omit<Base, 'abierto'> & {
  titulo: string
  alAceptar: () => void
  deshabilitado?: boolean
  textoAceptar: string
  children: ReactNode
}) {
  return (
    <Dialog open onClose={alCerrar} maxWidth="xs" fullWidth>
      <DialogTitle>{titulo}</DialogTitle>
      <DialogContent><Stack spacing={2} sx={{ pt: 1 }}>{children}</Stack></DialogContent>
      <DialogActions>
        <Button color="inherit" onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" onClick={alAceptar} loading={cargando} disabled={deshabilitado}>{textoAceptar}</Button>
      </DialogActions>
    </Dialog>
  )
}

const pesosAdorno = { input: { startAdornment: <InputAdornment position="start">$</InputAdornment> } }

// --- Pago ---

interface PropsPago extends Base {
  saldo: number
  mediosPago: string[]
  alRegistrar: (valor: number, medio: string) => void
}

/** Registro de un pago con uno de los medios aceptados por el alojamiento. */
export function DialogoPago(props: PropsPago) {
  return props.abierto ? <FormularioPago {...props} /> : null
}

function FormularioPago({ cargando, alCerrar, saldo, mediosPago, alRegistrar }: PropsPago) {
  const [valor, setValor] = useState(saldo > 0 ? String(saldo) : '')
  const [medio, setMedio] = useState(mediosPago[0] ?? '')
  const numero = Number(valor)
  return (
    <Marco cargando={cargando} alCerrar={alCerrar} titulo="Registrar pago" textoAceptar="Registrar pago"
           deshabilitado={!(numero > 0) || !medio} alAceptar={() => alRegistrar(numero, medio)}>
      <Typography variant="body2" color="text.secondary">Saldo actual: {formatoPesos(saldo)}</Typography>
      <TextField label="Valor" type="number" value={valor} onChange={(e) => setValor(e.target.value)} slotProps={pesosAdorno} />
      <TextField select label="Medio de pago" value={medio} onChange={(e) => setMedio(e.target.value)}>
        {mediosPago.map((m) => <MenuItem key={m} value={m}>{m.charAt(0) + m.slice(1).toLowerCase()}</MenuItem>)}
      </TextField>
    </Marco>
  )
}

// --- Servicio adicional ---

interface PropsServicio extends Base {
  alojamiento: Alojamiento | undefined
  alCargar: (codigo: string, cantidad: number) => void
}

/** Cargo por un servicio adicional configurado en el alojamiento. */
export function DialogoServicio(props: PropsServicio) {
  return props.abierto ? <FormularioServicio {...props} /> : null
}

function FormularioServicio({ cargando, alCerrar, alojamiento, alCargar }: PropsServicio) {
  const conCargo = alojamiento?.servicios.filter((s) => s.generaCargo) ?? []
  const [codigo, setCodigo] = useState(conCargo[0]?.codigo ?? '')
  const [cantidad, setCantidad] = useState(1)
  const servicio = conCargo.find((s) => s.codigo === codigo)
  return (
    <Marco cargando={cargando} alCerrar={alCerrar} titulo="Cargar servicio adicional" textoAceptar="Cargar"
           deshabilitado={!servicio || cantidad < 1} alAceptar={() => alCargar(codigo, cantidad)}>
      <TextField select label="Servicio" value={codigo} onChange={(e) => setCodigo(e.target.value)}>
        {conCargo.map((s) => <MenuItem key={s.codigo} value={s.codigo}>{s.nombre} · {formatoPesos(s.valor)} {s.unidad}</MenuItem>)}
      </TextField>
      <TextField label="Cantidad" type="number" value={cantidad} slotProps={{ htmlInput: { min: 1 } }}
                 onChange={(e) => setCantidad(Number(e.target.value))} />
      {servicio && <Typography>Total del cargo: {formatoPesos(servicio.valor * cantidad)}</Typography>}
    </Marco>
  )
}

// --- Ajuste ---

interface PropsAjuste extends Base {
  alRegistrar: (valor: number, descripcion: string) => void
}

/** Ajuste manual, positivo o negativo. */
export function DialogoAjuste(props: PropsAjuste) {
  return props.abierto ? <FormularioAjuste {...props} /> : null
}

function FormularioAjuste({ cargando, alCerrar, alRegistrar }: PropsAjuste) {
  const [valor, setValor] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const numero = Number(valor)
  return (
    <Marco cargando={cargando} alCerrar={alCerrar} titulo="Registrar ajuste" textoAceptar="Registrar"
           deshabilitado={!numero || !descripcion.trim()} alAceptar={() => alRegistrar(numero, descripcion)}>
      <TextField label="Valor" type="number" value={valor} onChange={(e) => setValor(e.target.value)} slotProps={pesosAdorno}
                 helperText="Use un valor negativo para un descuento o una compensación." />
      <TextField label="Descripción" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} multiline minRows={2} />
    </Marco>
  )
}

// --- Motivo (reversos y autorizaciones) ---

interface PropsMotivo extends Base {
  titulo: string
  descripcion?: ReactNode
  etiquetaMotivo: string
  textoAceptar: string
  obligatorio?: boolean
  alAceptar: (motivo: string) => void
}

/** Motivo de un reverso o de una autorización del administrador. */
export function DialogoMotivo(props: PropsMotivo) {
  return props.abierto ? <FormularioMotivo {...props} /> : null
}

function FormularioMotivo({ cargando, alCerrar, titulo, descripcion, etiquetaMotivo, textoAceptar, obligatorio = true, alAceptar }: PropsMotivo) {
  const [motivo, setMotivo] = useState('')
  return (
    <Marco cargando={cargando} alCerrar={alCerrar} titulo={titulo} textoAceptar={textoAceptar}
           deshabilitado={obligatorio && !motivo.trim()} alAceptar={() => alAceptar(motivo.trim())}>
      {descripcion && <Alert severity="info">{descripcion}</Alert>}
      <TextField label={etiquetaMotivo} value={motivo} onChange={(e) => setMotivo(e.target.value)} multiline minRows={2} />
    </Marco>
  )
}

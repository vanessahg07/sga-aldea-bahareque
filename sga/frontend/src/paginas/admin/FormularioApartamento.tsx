import { useState } from 'react'
import { Chip, FormControlLabel, Grid, Stack, Switch, TextField } from '@mui/material'
import type { DatosApartamento } from '../../api/servicios'
import { etiqueta } from '../../utilidades/formato'

export type DatosFormulario = DatosApartamento & { codigo: string }

export const apartamentoVacio: DatosFormulario = {
  codigo: '', nombre: '', descripcion: '', dormitorios: 1, capacidad: 2, caracteristicas: [], admiteMascotas: false,
}

/** Datos descriptivos de un apartamento. Las reglas (capacidad, dormitorios) las valida el dominio. */
export function FormularioApartamento({ datos, alCambiar, conCodigo }: {
  datos: DatosFormulario
  alCambiar: (datos: DatosFormulario) => void
  conCodigo?: boolean
}) {
  const [nueva, setNueva] = useState('')
  const agregar = () => {
    const valor = nueva.trim().toLowerCase().replace(/\s+/g, '-')
    if (valor && !datos.caracteristicas.includes(valor)) alCambiar({ ...datos, caracteristicas: [...datos.caracteristicas, valor] })
    setNueva('')
  }
  return (
    <Grid container spacing={2}>
      {conCodigo && (
        <Grid size={{ xs: 12, sm: 4 }}>
          <TextField label="Código" required value={datos.codigo} placeholder="BH-401"
                     onChange={(e) => alCambiar({ ...datos, codigo: e.target.value.toUpperCase() })} />
        </Grid>
      )}
      <Grid size={{ xs: 12, sm: conCodigo ? 8 : 12 }}>
        <TextField label="Nombre" required value={datos.nombre} onChange={(e) => alCambiar({ ...datos, nombre: e.target.value })} />
      </Grid>
      <Grid size={12}>
        <TextField label="Descripción" multiline minRows={2} value={datos.descripcion}
                   onChange={(e) => alCambiar({ ...datos, descripcion: e.target.value })} />
      </Grid>
      <Grid size={{ xs: 6, sm: 4 }}>
        <TextField label="Dormitorios" type="number" value={datos.dormitorios} slotProps={{ htmlInput: { min: 1 } }}
                   onChange={(e) => alCambiar({ ...datos, dormitorios: Number(e.target.value) })} />
      </Grid>
      <Grid size={{ xs: 6, sm: 4 }}>
        <TextField label="Capacidad" type="number" value={datos.capacidad} slotProps={{ htmlInput: { min: 1 } }}
                   onChange={(e) => alCambiar({ ...datos, capacidad: Number(e.target.value) })} />
      </Grid>
      <Grid size={{ xs: 12, sm: 4 }}>
        <FormControlLabel label="Admite mascotas"
                          control={<Switch checked={datos.admiteMascotas} onChange={(e) => alCambiar({ ...datos, admiteMascotas: e.target.checked })} />} />
      </Grid>
      <Grid size={12}>
        <TextField label="Agregar característica" value={nueva} onChange={(e) => setNueva(e.target.value)} placeholder="Ej.: chimenea"
                   onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); agregar() } }}
                   helperText="Escriba y presione Enter." />
        <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 0.75, mt: 1 }}>
          {datos.caracteristicas.map((c) => (
            <Chip key={c} label={etiqueta(c)} onDelete={() => alCambiar({ ...datos, caracteristicas: datos.caracteristicas.filter((x) => x !== c) })} />
          ))}
        </Stack>
      </Grid>
    </Grid>
  )
}

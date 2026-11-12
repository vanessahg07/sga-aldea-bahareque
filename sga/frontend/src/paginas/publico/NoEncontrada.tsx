import { Link as RouterLink } from 'react-router'
import { Button, Container, Typography } from '@mui/material'

export function NoEncontrada() {
  return (
    <Container maxWidth="sm" sx={{ py: 10, textAlign: 'center' }}>
      <Typography variant="h2" component="h1">Página no encontrada</Typography>
      <Typography color="text.secondary" sx={{ my: 2 }}>La dirección que buscó no existe.</Typography>
      <Button variant="contained" component={RouterLink} to="/">Volver al inicio</Button>
    </Container>
  )
}

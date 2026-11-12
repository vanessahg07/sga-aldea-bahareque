import { createTheme } from '@mui/material/styles'
import '@fontsource/roboto/400.css'
import '@fontsource/roboto/500.css'
import '@fontsource/roboto/700.css'

/**
 * Paleta "Bahareque": los materiales de la casa quindiana.
 * Cafetal para la estructura, teja para las acciones principales, guadua como acento
 * y cal como fondo.
 */
export const colores = {
  cafetal: '#34533A',
  cafetalOscuro: '#24392A',
  cafetalClaro: '#E4ECE2',
  teja: '#B4532F',
  tejaOscura: '#7A3219',
  tejaClara: '#F4DDD3',
  guadua: '#C9A55A',
  guaduaOscura: '#6E5217',
  guaduaClara: '#F6E6C9',
  cal: '#F5F0E6',
  papel: '#FFFDF8',
  borde: '#E3D9C6',
  texto: '#2A231D',
  textoSuave: '#6B5E52',
  niebla: '#D8E3EA',
  nieblaOscura: '#25404F',
}

/** Roboto en toda la interfaz: legible en pantallas pequeñas y familiar para el usuario. */
export const fuente = '"Roboto", "Segoe UI", system-ui, sans-serif'

export const tema = createTheme({
  palette: {
    primary: { main: colores.cafetal, dark: colores.cafetalOscuro, contrastText: colores.papel },
    secondary: { main: colores.teja, dark: colores.tejaOscura, contrastText: colores.papel },
    warning: { main: colores.guadua, contrastText: colores.texto },
    error: { main: '#A8321E' },
    success: { main: '#3F6B45' },
    info: { main: '#3E6A78' },
    background: { default: colores.cal, paper: colores.papel },
    text: { primary: colores.texto, secondary: colores.textoSuave },
    divider: colores.borde,
  },
  shape: { borderRadius: 8 },
  typography: {
    fontFamily: fuente,
    h1: { fontWeight: 700, fontSize: '2.5rem', lineHeight: 1.15, letterSpacing: '-0.01em' },
    h2: { fontWeight: 700, fontSize: '1.95rem', letterSpacing: '-0.005em' },
    h3: { fontWeight: 700, fontSize: '1.55rem' },
    h4: { fontWeight: 500, fontSize: '1.3rem' },
    h5: { fontWeight: 500, fontSize: '1.12rem' },
    h6: { fontWeight: 500, fontSize: '1rem' },
    button: { textTransform: 'none', fontWeight: 500 },
    overline: { letterSpacing: '0.1em', fontWeight: 500 },
  },
  components: {
    MuiAppBar: { defaultProps: { elevation: 0 } },
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiPaper: { styleOverrides: { outlined: { borderColor: colores.borde } } },
    MuiCard: {
      defaultProps: { variant: 'outlined' },
      styleOverrides: { root: { borderColor: colores.borde } },
    },
    MuiTableCell: {
      styleOverrides: {
        head: { fontWeight: 500, color: colores.textoSuave, backgroundColor: colores.cal },
        root: { borderColor: colores.borde },
      },
    },
    MuiChip: { styleOverrides: { root: { fontWeight: 500 } } },
    MuiTextField: { defaultProps: { size: 'small', fullWidth: true } },
    MuiSelect: { defaultProps: { size: 'small' } },
  },
})

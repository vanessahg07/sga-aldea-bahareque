import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// En desarrollo, la API y las imágenes subidas (/archivos) se piden al backend de Spring Boot.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:9090',
      '/archivos': 'http://localhost:9090',
    },
  },
})

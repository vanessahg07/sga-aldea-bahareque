package co.edu.uniquindio.sga.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga.application.apartamento.ImagenServicio;
import co.edu.uniquindio.sga.application.apartamento.SugerenciaGravedadServicio;
import co.edu.uniquindio.sga.domain.apartamento.Novedad;
import co.edu.uniquindio.sga.infrastructure.ia.ConfiguracionIa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Integraciones del inventario: sugerencia de gravedad con IA y subida de imágenes. */
@RestController
@RequestMapping("/api")
public class IntegracionesInventarioControlador {

    public record SugerenciaSolicitud(@NotBlank String descripcion) {
    }

    /**
     * @param disponible {@code false} si la IA está deshabilitada, falló o no respondió a tiempo;
     *                   en ese caso el personal elige la gravedad sin ayuda
     */
    public record SugerenciaR(boolean disponible, Novedad.Gravedad gravedad, String justificacion, String proveedor,
                              String mensaje) {
    }

    public record ImagenSubidaR(String url) {
    }

    private final SugerenciaGravedadServicio sugerencias;
    private final ImagenServicio imagenes;
    private final ConfiguracionIa.EstadoIa estadoIa;

    public IntegracionesInventarioControlador(SugerenciaGravedadServicio sugerencias, ImagenServicio imagenes,
                                              ConfiguracionIa.EstadoIa estadoIa) {
        this.sugerencias = sugerencias;
        this.imagenes = imagenes;
        this.estadoIa = estadoIa;
    }

    @GetMapping("/novedades/ia")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'PERSONAL_SERVICIO')")
    public ConfiguracionIa.EstadoIa estadoIa() {
        return estadoIa;
    }

    @PostMapping("/novedades/sugerencia-gravedad")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'PERSONAL_SERVICIO')")
    public SugerenciaR sugerirGravedad(@Valid @RequestBody SugerenciaSolicitud s) {
        return sugerencias.sugerir(s.descripcion())
                .map(g -> new SugerenciaR(true, g.gravedad(), g.justificacion(), g.proveedor(), null))
                .orElseGet(() -> new SugerenciaR(false, null, null, null, estadoIa.habilitada()
                        ? "La sugerencia de IA no está disponible en este momento. Elija la gravedad manualmente."
                        : "La IA está deshabilitada. Elija la gravedad manualmente."));
    }

    @PostMapping("/imagenes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ImagenSubidaR subirImagen(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        return new ImagenSubidaR(imagenes.subir(archivo.getOriginalFilename(), archivo.getContentType(),
                archivo.getBytes()));
    }
}

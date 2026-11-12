package co.edu.uniquindio.sga.infrastructure.rest.reserva;

import co.edu.uniquindio.sga.application.alojamiento.AlojamientoServicio;
import co.edu.uniquindio.sga.application.reserva.DisponibilidadServicio;
import co.edu.uniquindio.sga.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga.infrastructure.rest.comun.PaginaRespuesta;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.ResultadoCotizacionR;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.TarjetaR;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Búsqueda de apartamentos disponibles y cotización de estancias. Son consultas públicas. */
@RestController
@RequestMapping("/api")
public class DisponibilidadControlador {

    /**
     * El grupo se describe con las fechas de nacimiento; si solo se conoce la cantidad de
     * personas, se estima como si todas fueran facturables.
     */
    public record BusquedaSolicitud(@NotNull LocalDate entrada, @NotNull LocalDate salida,
                                    List<LocalDate> fechasNacimiento, @Min(1) Integer personas,
                                    @Min(1) Integer capacidadMinima, BigDecimal precioMinimo,
                                    BigDecimal precioMaximo, Set<String> caracteristicas, @Min(0) int mascotas) {
    }

    public record CotizacionSolicitud(@NotBlank String codigoApartamento, @NotNull LocalDate entrada,
                                      @NotNull LocalDate salida, List<LocalDate> fechasNacimiento,
                                      @Min(1) Integer personas, @Min(0) int mascotas) {
    }

    private final DisponibilidadServicio disponibilidad;
    private final AlojamientoServicio alojamiento;

    public DisponibilidadControlador(DisponibilidadServicio disponibilidad, AlojamientoServicio alojamiento) {
        this.disponibilidad = disponibilidad;
        this.alojamiento = alojamiento;
    }

    @PostMapping("/disponibilidad/busqueda")
    public PaginaRespuesta<TarjetaR> buscar(@Valid @RequestBody BusquedaSolicitud s,
                                            @RequestParam(defaultValue = "0") int pagina) {
        Alojamiento datos = alojamiento.consultar();
        return PaginaRespuesta.de(disponibilidad.buscar(new DisponibilidadServicio.Criterios(s.entrada(), s.salida(),
                s.fechasNacimiento(), s.personas(), s.capacidadMinima(), s.precioMinimo(), s.precioMaximo(),
                s.caracteristicas(), s.mascotas()), pagina), r -> TarjetaR.de(r, datos));
    }

    @PostMapping("/cotizaciones")
    public ResultadoCotizacionR cotizar(@Valid @RequestBody CotizacionSolicitud s) {
        return ResultadoCotizacionR.de(disponibilidad.cotizar(s.codigoApartamento(), s.entrada(), s.salida(),
                s.fechasNacimiento(), s.personas(), s.mascotas()));
    }
}

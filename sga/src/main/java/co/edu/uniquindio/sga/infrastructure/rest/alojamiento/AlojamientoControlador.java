package co.edu.uniquindio.sga.infrastructure.rest.alojamiento;

import co.edu.uniquindio.sga.application.alojamiento.AlojamientoServicio;
import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.folio.MedioPago;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.AlojamientoR;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/** Datos, normas, servicios, medios de pago y parámetros del alojamiento. */
@RestController
@RequestMapping("/api/alojamiento")
public class AlojamientoControlador {

    public record DatosSolicitud(@NotBlank String nombre, String descripcion, @NotBlank String ciudad,
                                 String direccion, @NotNull BigDecimal latitud, @NotNull BigDecimal longitud,
                                 List<String> normas) {
    }

    public record ParametrosSolicitud(@NotNull @Min(0) Integer umbralEdadFacturable, @NotNull LocalTime horaEntrada,
                                      @NotNull LocalTime horaSalida, @NotNull @Min(0) Integer horasPreparacion,
                                      @NotNull @Min(1) Integer horasPlazoConfirmacion,
                                      @NotNull LocalTime horaLimiteNoShow, @NotNull BigDecimal porcentajeAnticipo,
                                      @NotNull @Min(0) Integer maxMascotas, @NotNull BigDecimal cargoMascota,
                                      @NotNull @Min(1) Integer nochesDescuentoLargo,
                                      @NotNull BigDecimal porcentajeDescuentoLargo,
                                      @Min(0) Integer diasRecordatorioLlegada) {
    }

    public record ServicioSolicitud(@NotBlank String codigo, @NotBlank String nombre, String descripcion,
                                    boolean generaCargo, BigDecimal valor, String unidad) {
    }

    public record MediosPagoSolicitud(@NotEmpty List<@NotBlank String> medios) {
    }

    private final AlojamientoServicio alojamiento;

    public AlojamientoControlador(AlojamientoServicio alojamiento) {
        this.alojamiento = alojamiento;
    }

    @GetMapping
    public AlojamientoR consultar() {
        return AlojamientoR.de(alojamiento.consultar());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AlojamientoR actualizarDatos(@Valid @RequestBody DatosSolicitud s) {
        return AlojamientoR.de(alojamiento.actualizarDatos(s.nombre(), s.descripcion(), s.ciudad(), s.direccion(),
                s.latitud(), s.longitud(), s.normas()));
    }

    @PutMapping("/parametros")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AlojamientoR cambiarParametros(@Valid @RequestBody ParametrosSolicitud s) {
        return AlojamientoR.de(alojamiento.cambiarParametros(new ParametrosAlojamiento(s.umbralEdadFacturable(),
                s.horaEntrada(), s.horaSalida(), s.horasPreparacion(), s.horasPlazoConfirmacion(),
                s.horaLimiteNoShow(), s.porcentajeAnticipo(), s.maxMascotas(), Dinero.de(s.cargoMascota()),
                s.nochesDescuentoLargo(), s.porcentajeDescuentoLargo(),
                s.diasRecordatorioLlegada() != null ? s.diasRecordatorioLlegada()
                        : alojamiento.consultar().getParametros().diasRecordatorioLlegada())));
    }

    @PutMapping("/servicios")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AlojamientoR definirServicios(@Valid @RequestBody List<@Valid ServicioSolicitud> servicios) {
        return AlojamientoR.de(alojamiento.definirServicios(servicios.stream()
                .map(s -> new ServicioAdicional(s.codigo(), s.nombre(), s.descripcion(), s.generaCargo(),
                        s.valor() == null ? null : Dinero.de(s.valor()), s.unidad()))
                .toList()));
    }

    @PutMapping("/medios-pago")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AlojamientoR definirMediosPago(@Valid @RequestBody MediosPagoSolicitud s) {
        return AlojamientoR.de(alojamiento.definirMediosPago(
                s.medios().stream().map(MedioPago::new).collect(Collectors.toSet())));
    }
}

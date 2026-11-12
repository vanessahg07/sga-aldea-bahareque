package co.edu.uniquindio.sga.infrastructure.rest.tarifa;

import co.edu.uniquindio.sga.application.tarifa.TarifaServicio;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;
import co.edu.uniquindio.sga.domain.politica.TramoCancelacion;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.PoliticaR;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.TarifaR;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.TemporadaR;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Temporadas, tarifas y política de cancelación. */
@RestController
@RequestMapping("/api")
public class TarifaControlador {

    public record RangoSolicitud(@NotNull LocalDate inicio, @NotNull LocalDate fin) {
    }

    public record TemporadaSolicitud(@NotBlank String nombre, List<@Valid RangoSolicitud> rangos,
                                     @Min(1) int estanciaMinimaNoches) {
    }

    public record ActualizacionTemporadaSolicitud(List<@Valid RangoSolicitud> rangos, @Min(1) int estanciaMinimaNoches) {
    }

    public record TarifaSolicitud(@NotBlank String codigoApartamento, @NotBlank String temporada,
                                  @NotNull @Positive BigDecimal valor) {
    }

    public record TramoSolicitud(@Min(0) int diasAntelacionMinimos, @NotNull BigDecimal porcentajeRetencion) {
    }

    public record PoliticaSolicitud(@NotNull @Size(min = 2) List<@Valid TramoSolicitud> tramos,
                                    @NotNull BigDecimal porcentajeRetencionNoShow) {
    }

    private final TarifaServicio tarifas;

    public TarifaControlador(TarifaServicio tarifas) {
        this.tarifas = tarifas;
    }

    // --- Temporadas ---

    @GetMapping("/temporadas")
    public List<TemporadaR> temporadas() {
        return tarifas.temporadas().stream().map(TemporadaR::de).toList();
    }

    @PostMapping("/temporadas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TemporadaR crearTemporada(@Valid @RequestBody TemporadaSolicitud s) {
        return TemporadaR.de(tarifas.crearTemporada(s.nombre(), rangos(s.rangos()), s.estanciaMinimaNoches()));
    }

    @PutMapping("/temporadas/{nombre}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TemporadaR actualizarTemporada(@PathVariable String nombre,
                                          @Valid @RequestBody ActualizacionTemporadaSolicitud s) {
        return TemporadaR.de(tarifas.actualizarTemporada(nombre, rangos(s.rangos()), s.estanciaMinimaNoches()));
    }

    @DeleteMapping("/temporadas/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminarTemporada(@PathVariable String nombre) {
        tarifas.eliminarTemporada(nombre);
    }

    // --- Tarifas ---

    @PostMapping("/tarifas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TarifaR registrarTarifa(@Valid @RequestBody TarifaSolicitud s) {
        return TarifaR.de(tarifas.registrarTarifa(s.codigoApartamento(), s.temporada(), s.valor()));
    }

    @GetMapping("/tarifas/historico/{codigoApartamento}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<TarifaR> historico(@PathVariable String codigoApartamento) {
        return tarifas.historico(codigoApartamento).stream().map(TarifaR::de).toList();
    }

    /** Apartamentos activos sin tarifa en alguna temporada: no pueden venderse hasta completarlas. */
    @GetMapping("/tarifas/incompletas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<String> apartamentosConTarifasIncompletas() {
        return tarifas.apartamentosConTarifasIncompletas();
    }

    // --- Política de cancelación ---

    @GetMapping("/politicas/vigente")
    public PoliticaR politicaVigente() {
        return PoliticaR.de(tarifas.politicaVigente());
    }

    @GetMapping("/politicas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<PoliticaR> versiones() {
        return tarifas.versionesPolitica().stream().map(PoliticaR::de).toList();
    }

    /** Publica una nueva versión; las reservas existentes conservan la suya. */
    @PostMapping("/politicas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PoliticaR publicar(@Valid @RequestBody PoliticaSolicitud s) {
        return PoliticaR.de(tarifas.publicarPolitica(
                s.tramos().stream().map(t -> new TramoCancelacion(t.diasAntelacionMinimos(), t.porcentajeRetencion())).toList(),
                s.porcentajeRetencionNoShow()));
    }

    private static List<RangoFechas> rangos(List<RangoSolicitud> rangos) {
        return rangos == null ? List.of() : rangos.stream().map(r -> new RangoFechas(r.inicio(), r.fin())).toList();
    }
}

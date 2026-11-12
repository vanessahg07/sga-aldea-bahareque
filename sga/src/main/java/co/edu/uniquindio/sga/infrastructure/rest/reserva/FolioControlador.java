package co.edu.uniquindio.sga.infrastructure.rest.reserva;

import co.edu.uniquindio.sga.application.reserva.FolioServicio;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.FolioR;
import co.edu.uniquindio.sga.infrastructure.seguridad.SolicitanteActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Folio de una reserva: cargos, pagos, reversos y cierre. Los movimientos nunca se editan ni se borran. */
@RestController
@RequestMapping("/api/reservas/{idReserva}/folio")
public class FolioControlador {

    private static final String PERSONAL = "hasAnyRole('RECEPCIONISTA', 'ADMINISTRADOR')";

    public record PagoSolicitud(@NotNull @Positive BigDecimal valor, @NotBlank String medio, LocalDateTime fecha) {
    }

    public record ServicioSolicitud(@NotBlank String codigoServicio, @Min(1) int cantidad) {
    }

    public record AjusteSolicitud(@NotNull BigDecimal valor, @NotBlank String descripcion) {
    }

    public record ReversoSolicitud(@NotBlank String motivo) {
    }

    public record CierreSolicitud(String motivoAutorizacion) {
    }

    private final FolioServicio folios;
    private final SolicitanteActual solicitante;

    public FolioControlador(FolioServicio folios, SolicitanteActual solicitante) {
        this.folios = folios;
        this.solicitante = solicitante;
    }

    /** El huésped solo consulta el folio de sus propias reservas. */
    @GetMapping
    public FolioR consultar(@PathVariable String idReserva, @AuthenticationPrincipal Jwt jwt) {
        return FolioR.de(folios.consultar(idReserva, solicitante.de(jwt)));
    }

    @PostMapping("/pagos")
    @PreAuthorize(PERSONAL)
    public FolioR registrarPago(@PathVariable String idReserva, @Valid @RequestBody PagoSolicitud s) {
        return FolioR.de(folios.registrarPago(idReserva, s.valor(), s.medio(), s.fecha()));
    }

    @PostMapping("/servicios")
    @PreAuthorize(PERSONAL)
    public FolioR cargarServicio(@PathVariable String idReserva, @Valid @RequestBody ServicioSolicitud s) {
        return FolioR.de(folios.cargarServicio(idReserva, s.codigoServicio(), s.cantidad()));
    }

    @PostMapping("/ajustes")
    @PreAuthorize(PERSONAL)
    public FolioR registrarAjuste(@PathVariable String idReserva, @Valid @RequestBody AjusteSolicitud s) {
        return FolioR.de(folios.registrarAjuste(idReserva, s.valor(), s.descripcion()));
    }

    @PostMapping("/cargos/{idCargo}/reverso")
    @PreAuthorize(PERSONAL)
    public FolioR revertirCargo(@PathVariable String idReserva, @PathVariable String idCargo,
                                @Valid @RequestBody ReversoSolicitud s) {
        return FolioR.de(folios.revertirCargo(idReserva, idCargo, s.motivo()));
    }

    @PostMapping("/pagos/{idPago}/reverso")
    @PreAuthorize(PERSONAL)
    public FolioR revertirPago(@PathVariable String idReserva, @PathVariable String idPago) {
        return FolioR.de(folios.revertirPago(idReserva, idPago));
    }

    /** Con saldo distinto de cero solo el administrador puede cerrarlo, indicando el motivo. */
    @PostMapping("/cierre")
    @PreAuthorize(PERSONAL)
    public FolioR cerrar(@PathVariable String idReserva, @RequestBody(required = false) CierreSolicitud s,
                         @AuthenticationPrincipal Jwt jwt) {
        return FolioR.de(folios.cerrar(idReserva, s == null ? null : s.motivoAutorizacion(), solicitante.de(jwt)));
    }
}

package co.edu.uniquindio.sga.infrastructure.rest.reserva;

import co.edu.uniquindio.sga.application.reserva.ReservaServicio;
import co.edu.uniquindio.sga.domain.reserva.*;
import co.edu.uniquindio.sga.infrastructure.rest.comun.PaginaRespuesta;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.ReservaR;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.ReservaResumenR;
import co.edu.uniquindio.sga.infrastructure.seguridad.SolicitanteActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Ciclo de vida de la reserva: creación por portal y directa, modificación, transiciones y consultas. */
@RestController
@RequestMapping("/api/reservas")
public class ReservaControlador {

    private static final String PERSONAL = "hasAnyRole('RECEPCIONISTA', 'ADMINISTRADOR')";

    public record OcupanteSolicitud(@NotBlank String nombre, @NotNull @Past LocalDate fechaNacimiento) {
    }

    public record TitularSolicitud(@NotBlank String nombre, @NotBlank String documento, @Email String correo,
                                   String telefono, @NotNull @Past LocalDate fechaNacimiento) {
    }

    /** Reserva por el portal: el titular es el huésped autenticado. */
    public record ReservaPortalSolicitud(@NotBlank String codigoApartamento, @NotNull LocalDate entrada,
                                         @NotNull LocalDate salida, List<@Valid OcupanteSolicitud> acompanantes,
                                         @NotNull LocalTime horaEstimadaLlegada, @Min(0) int mascotas) {
    }

    /** Reserva directa: la recepción indica el titular, que puede no tener cuenta. */
    public record ReservaDirectaSolicitud(@NotNull @Valid TitularSolicitud titular, @NotBlank String codigoApartamento,
                                          @NotNull LocalDate entrada, @NotNull LocalDate salida,
                                          List<@Valid OcupanteSolicitud> acompanantes, LocalTime horaEstimadaLlegada,
                                          @Min(0) int mascotas) {
    }

    /** Modificación: si no se indica apartamento, se conserva el actual. */
    public record ModificacionSolicitud(String codigoApartamento, @NotNull LocalDate entrada, @NotNull LocalDate salida,
                                        List<@Valid OcupanteSolicitud> acompanantes, LocalTime horaEstimadaLlegada,
                                        @Min(0) int mascotas) {
    }

    public record HoraLlegadaSolicitud(@NotNull LocalTime horaEstimadaLlegada) {
    }

    public record SalidaSolicitud(String motivoAutorizacion) {
    }

    private final ReservaServicio reservas;
    private final SolicitanteActual solicitante;

    public ReservaControlador(ReservaServicio reservas, SolicitanteActual solicitante) {
        this.reservas = reservas;
        this.solicitante = solicitante;
    }

    // --- Creación ---

    @PostMapping("/portal")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('HUESPED')")
    public ReservaR crearPorPortal(@Valid @RequestBody ReservaPortalSolicitud s, @AuthenticationPrincipal Jwt jwt) {
        return ReservaR.de(reservas.crearPorPortal(new ReservaServicio.DatosEstancia(s.codigoApartamento(), s.entrada(),
                s.salida(), ocupantes(s.acompanantes()), s.horaEstimadaLlegada(), s.mascotas()), solicitante.de(jwt)));
    }

    @PostMapping("/directas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(PERSONAL)
    public ReservaR crearDirecta(@Valid @RequestBody ReservaDirectaSolicitud s) {
        TitularSolicitud t = s.titular();
        return ReservaR.de(reservas.crearDirecta(new ReservaServicio.DatosEstancia(s.codigoApartamento(), s.entrada(),
                s.salida(), ocupantes(s.acompanantes()), s.horaEstimadaLlegada(), s.mascotas()),
                new Titular(t.nombre(), t.documento(), t.correo(), t.telefono(), t.fechaNacimiento())));
    }

    // --- Consultas ---

    /** Listado para el personal, de la más reciente a la más antigua, con filtros opcionales. */
    @GetMapping
    @PreAuthorize(PERSONAL)
    public PaginaRespuesta<ReservaResumenR> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) String apartamento,
            @RequestParam(required = false) Canal canal,
            @RequestParam(required = false) String titular,
            @RequestParam(defaultValue = "0") int pagina) {
        return PaginaRespuesta.de(reservas.listar(new FiltroReservas(desde, hasta, estado, apartamento, canal, titular,
                null), pagina), ReservaResumenR::de);
    }

    /** Historial del huésped autenticado. */
    @GetMapping("/mias")
    @PreAuthorize("hasRole('HUESPED')")
    public PaginaRespuesta<ReservaResumenR> mias(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam(defaultValue = "0") int pagina) {
        return PaginaRespuesta.de(reservas.mias(solicitante.de(jwt), pagina), ReservaResumenR::de);
    }

    @GetMapping("/llegadas")
    @PreAuthorize(PERSONAL)
    public List<ReservaResumenR> llegadas(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return reservas.llegadas(fecha).stream().map(ReservaResumenR::de).toList();
    }

    @GetMapping("/salidas")
    @PreAuthorize(PERSONAL)
    public List<ReservaResumenR> salidas(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return reservas.salidas(fecha).stream().map(ReservaResumenR::de).toList();
    }

    /** Calendario de ocupación: reservas activas que tocan el rango, por apartamento y fecha. */
    @GetMapping("/ocupacion")
    @PreAuthorize(PERSONAL)
    public List<ReservaResumenR> ocupacion(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reservas.ocupacion(desde, hasta).stream().map(ReservaResumenR::de).toList();
    }

    /** Un huésped solo puede consultar sus propias reservas. */
    @GetMapping("/{id}")
    public ReservaR consultar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ReservaR.de(reservas.consultar(id, solicitante.de(jwt)));
    }

    // --- Modificación y transiciones ---

    @PutMapping("/{id}")
    @PreAuthorize(PERSONAL)
    public ReservaR modificar(@PathVariable String id, @Valid @RequestBody ModificacionSolicitud s) {
        return ReservaR.de(reservas.modificar(id, new ReservaServicio.DatosEstancia(s.codigoApartamento(), s.entrada(),
                s.salida(), ocupantes(s.acompanantes()), s.horaEstimadaLlegada(), s.mascotas())));
    }

    @PutMapping("/{id}/hora-llegada")
    public ReservaR registrarHoraLlegada(@PathVariable String id, @Valid @RequestBody HoraLlegadaSolicitud s,
                                         @AuthenticationPrincipal Jwt jwt) {
        return ReservaR.de(reservas.registrarHoraLlegada(id, s.horaEstimadaLlegada(), solicitante.de(jwt)));
    }

    @PostMapping("/{id}/confirmacion")
    @PreAuthorize(PERSONAL)
    public ReservaR confirmar(@PathVariable String id) {
        return ReservaR.de(reservas.confirmar(id));
    }

    /** El huésped cancela sus propias reservas; el personal, cualquiera. */
    @PostMapping("/{id}/cancelacion")
    public ReservaR cancelar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ReservaR.de(reservas.cancelar(id, solicitante.de(jwt)));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize(PERSONAL)
    public ReservaR declararNoShow(@PathVariable String id) {
        return ReservaR.de(reservas.declararNoShow(id));
    }

    @PostMapping("/{id}/llegada")
    @PreAuthorize(PERSONAL)
    public ReservaR registrarLlegada(@PathVariable String id) {
        return ReservaR.de(reservas.registrarLlegada(id));
    }

    /** Si el folio tiene saldo, solo el administrador puede autorizar el cierre indicando el motivo. */
    @PostMapping("/{id}/salida")
    @PreAuthorize(PERSONAL)
    public ReservaR registrarSalida(@PathVariable String id, @RequestBody(required = false) SalidaSolicitud s,
                                    @AuthenticationPrincipal Jwt jwt) {
        return ReservaR.de(reservas.registrarSalida(id, s == null ? null : s.motivoAutorizacion(), solicitante.de(jwt)));
    }

    private static List<Ocupante> ocupantes(List<OcupanteSolicitud> lista) {
        return lista == null ? List.of() : lista.stream().map(o -> new Ocupante(o.nombre(), o.fechaNacimiento())).toList();
    }
}

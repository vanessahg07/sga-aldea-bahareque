package co.edu.uniquindio.sga.infrastructure.rest.canal;

import co.edu.uniquindio.sga.application.canal.CanalServicio;
import co.edu.uniquindio.sga.domain.canal.CanalVenta;
import co.edu.uniquindio.sga.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga.domain.canal.EventoCanal;
import co.edu.uniquindio.sga.domain.canal.TipoCanal;
import co.edu.uniquindio.sga.domain.canal.TipoEventoCanal;
import co.edu.uniquindio.sga.infrastructure.rest.comun.PaginaRespuesta;
import co.edu.uniquindio.sga.infrastructure.seguridad.SolicitanteActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Administración de canales de venta, conflictos y bitácora de eventos. */
@RestController
@RequestMapping("/api/canales")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CanalControlador {

    public record CanalSolicitud(@NotBlank String codigo, @NotBlank String nombre, @NotNull TipoCanal tipo) {
    }

    public record DatosCanalSolicitud(@NotBlank String nombre, @NotNull TipoCanal tipo) {
    }

    public record ActivoSolicitud(boolean activo) {
    }

    public record RevisionSolicitud(@NotBlank String nota) {
    }

    public record CanalR(String codigo, String nombre, TipoCanal tipo, boolean activo) {
        static CanalR de(CanalVenta c) {
            return new CanalR(c.getCodigo(), c.getNombre(), c.getTipo(), c.isActivo());
        }
    }

    /** La credencial en claro se entrega solo en esta respuesta. */
    public record CredencialR(CanalR canal, String credencial, String aviso) {
        static CredencialR de(CanalVenta.Credencial c) {
            return new CredencialR(CanalR.de(c.canal()), c.credencialEnClaro(),
                    "Guarde la credencial ahora: no volverá a mostrarse. Entréguela al canal por un medio seguro.");
        }
    }

    public record ConflictoAdminR(String id, String canal, String idExterno, String codigoApartamento, LocalDate entrada,
                                  LocalDate salida, String titular, String motivo, String mensaje,
                                  LocalDateTime registradoEn, boolean revisado, String revisadoPor, String notaRevision,
                                  LocalDateTime fechaRevision) {
        static ConflictoAdminR de(ConflictoCanal c) {
            ConflictoCanal.Revision r = c.revision();
            return new ConflictoAdminR(c.id(), c.referencia().canal(), c.referencia().idExterno(),
                    c.codigoApartamento(), c.estancia().fechaEntrada(), c.estancia().fechaSalida(), c.titular(),
                    c.motivo().name(), c.motivo().mensaje(), c.registradoEn(), r != null,
                    r == null ? null : r.autor(), r == null ? null : r.nota(), r == null ? null : r.fecha());
        }
    }

    public record EventoR(String id, String canal, TipoEventoCanal tipo, String idExterno, String idReserva,
                          String detalle, LocalDateTime fecha) {
        static EventoR de(EventoCanal e) {
            return new EventoR(e.id(), e.codigoCanal(), e.tipo(), e.idExterno(), e.idReserva(), e.detalle(), e.fecha());
        }
    }

    private final CanalServicio canales;
    private final SolicitanteActual solicitante;

    public CanalControlador(CanalServicio canales, SolicitanteActual solicitante) {
        this.canales = canales;
        this.solicitante = solicitante;
    }

    @GetMapping
    public List<CanalR> listar() {
        return canales.listar().stream().map(CanalR::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CredencialR registrar(@Valid @RequestBody CanalSolicitud s) {
        return CredencialR.de(canales.registrar(s.codigo(), s.nombre(), s.tipo()));
    }

    @PutMapping("/{codigo}")
    public CanalR actualizar(@PathVariable String codigo, @Valid @RequestBody DatosCanalSolicitud s) {
        return CanalR.de(canales.actualizar(codigo, s.nombre(), s.tipo()));
    }

    @PostMapping("/{codigo}/credencial")
    public CredencialR renovarCredencial(@PathVariable String codigo) {
        return CredencialR.de(canales.renovarCredencial(codigo));
    }

    @PutMapping("/{codigo}/activo")
    public CanalR cambiarActivo(@PathVariable String codigo, @RequestBody ActivoSolicitud s) {
        return CanalR.de(canales.cambiarActivo(codigo, s.activo()));
    }

    // --- Conflictos ---

    @GetMapping("/conflictos")
    public PaginaRespuesta<ConflictoAdminR> conflictos(@RequestParam(defaultValue = "false") boolean pendientes,
                                                       @RequestParam(defaultValue = "0") int pagina) {
        return PaginaRespuesta.de(canales.conflictos(pendientes, pagina), ConflictoAdminR::de);
    }

    @PostMapping("/conflictos/{id}/revision")
    public ConflictoAdminR revisar(@PathVariable String id, @Valid @RequestBody RevisionSolicitud s,
                                   @AuthenticationPrincipal Jwt jwt) {
        return ConflictoAdminR.de(canales.revisarConflicto(id, s.nota(), solicitante.de(jwt)));
    }

    // --- Bitácora ---

    @GetMapping("/bitacora")
    public PaginaRespuesta<EventoR> bitacora(@RequestParam(required = false) String canal,
                                             @RequestParam(defaultValue = "0") int pagina) {
        return PaginaRespuesta.de(canales.bitacora(canal, pagina), EventoR::de);
    }
}

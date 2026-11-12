package co.edu.uniquindio.sga.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga.application.apartamento.ApartamentoServicio;
import co.edu.uniquindio.sga.application.tarifa.TarifaServicio;
import co.edu.uniquindio.sga.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga.domain.apartamento.ImagenApartamento;
import co.edu.uniquindio.sga.domain.apartamento.Novedad;
import co.edu.uniquindio.sga.infrastructure.rest.comun.PaginaRespuesta;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.*;
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
import java.util.List;
import java.util.Set;

/** Inventario: apartamentos, imágenes, bloqueos, calendario, estado operativo y novedades. */
@RestController
@RequestMapping("/api/apartamentos")
public class ApartamentoControlador {

    public record ApartamentoSolicitud(@NotBlank String codigo, @NotBlank String nombre, String descripcion,
                                       @Min(1) int dormitorios, @Min(1) int capacidad, Set<String> caracteristicas,
                                       boolean admiteMascotas) {
    }

    public record ActualizacionSolicitud(@NotBlank String nombre, String descripcion, @Min(1) int dormitorios,
                                         @Min(1) int capacidad, Set<String> caracteristicas, boolean admiteMascotas) {
    }

    public record ImagenSolicitud(@NotBlank String url, boolean principal) {
    }

    public record BloqueoSolicitud(@NotNull LocalDate desde, @NotNull LocalDate hasta, @NotBlank String motivo) {
    }

    public record EstadoSolicitud(@NotNull EstadoOperativo estado) {
    }

    public record NovedadSolicitud(@NotBlank String descripcion, @NotNull Novedad.Gravedad gravedad) {
    }

    private final ApartamentoServicio apartamentos;
    private final TarifaServicio tarifas;
    private final SolicitanteActual solicitante;

    public ApartamentoControlador(ApartamentoServicio apartamentos, TarifaServicio tarifas,
                                  SolicitanteActual solicitante) {
        this.apartamentos = apartamentos;
        this.tarifas = tarifas;
        this.solicitante = solicitante;
    }

    // --- Consultas públicas ---

    /** Apartamentos a la venta, para el portal. */
    @GetMapping
    public PaginaRespuesta<ApartamentoR> listarALaVenta(@RequestParam(defaultValue = "0") int pagina) {
        return PaginaRespuesta.de(apartamentos.listarALaVenta(pagina), ApartamentoR::de);
    }

    @GetMapping("/{codigo}")
    public ApartamentoR consultar(@PathVariable String codigo) {
        return ApartamentoR.de(apartamentos.consultar(codigo));
    }

    @GetMapping("/{codigo}/calendario")
    public List<NocheR> calendario(@PathVariable String codigo,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return apartamentos.calendario(codigo, desde, hasta).stream().map(NocheR::de).toList();
    }

    /** Tarifas vigentes por temporada. */
    @GetMapping("/{codigo}/tarifas")
    public List<TarifaR> tarifasVigentes(@PathVariable String codigo) {
        apartamentos.consultar(codigo);
        return tarifas.vigentes(codigo).stream().map(TarifaR::de).toList();
    }

    // --- Gestión (administrador) ---

    /** Todos los apartamentos no eliminados, incluidos los inactivos. */
    @GetMapping("/gestion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public PaginaRespuesta<ApartamentoR> listarGestion(@RequestParam(defaultValue = "0") int pagina) {
        return PaginaRespuesta.de(apartamentos.listar(pagina), ApartamentoR::de);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApartamentoR crear(@Valid @RequestBody ApartamentoSolicitud s) {
        return ApartamentoR.de(apartamentos.crear(s.codigo(), new ApartamentoServicio.DatosApartamento(s.nombre(),
                s.descripcion(), s.dormitorios(), s.capacidad(), s.caracteristicas(), s.admiteMascotas())));
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ActualizacionApartamentoR actualizar(@PathVariable String codigo, @Valid @RequestBody ActualizacionSolicitud s) {
        return ActualizacionApartamentoR.de(apartamentos.actualizar(codigo, new ApartamentoServicio.DatosApartamento(
                s.nombre(), s.descripcion(), s.dormitorios(), s.capacidad(), s.caracteristicas(), s.admiteMascotas())));
    }

    @PutMapping("/{codigo}/imagenes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApartamentoR definirImagenes(@PathVariable String codigo,
                                        @Valid @RequestBody @Size(min = 1, max = 10) List<@Valid ImagenSolicitud> imagenes) {
        return ApartamentoR.de(apartamentos.definirImagenes(codigo,
                imagenes.stream().map(i -> new ImagenApartamento(i.url(), i.principal())).toList()));
    }

    @PostMapping("/{codigo}/activacion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApartamentoR activar(@PathVariable String codigo) {
        return ApartamentoR.de(apartamentos.activar(codigo));
    }

    @PostMapping("/{codigo}/desactivacion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApartamentoR desactivar(@PathVariable String codigo) {
        return ApartamentoR.de(apartamentos.desactivar(codigo));
    }

    /** Eliminación lógica: solo si no tiene reservas activas ni futuras. */
    @DeleteMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminar(@PathVariable String codigo) {
        apartamentos.eliminar(codigo);
    }

    // --- Bloqueos ---

    @GetMapping("/{codigo}/bloqueos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public List<BloqueoR> bloqueos(@PathVariable String codigo) {
        return apartamentos.bloqueosVigentes(codigo).stream().map(BloqueoR::de).toList();
    }

    @PostMapping("/{codigo}/bloqueos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public BloqueoR registrarBloqueo(@PathVariable String codigo, @Valid @RequestBody BloqueoSolicitud s) {
        return BloqueoR.de(apartamentos.registrarBloqueo(codigo, s.desde(), s.hasta(), s.motivo()));
    }

    @DeleteMapping("/bloqueos/{idBloqueo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void levantarBloqueo(@PathVariable String idBloqueo) {
        apartamentos.levantarBloqueo(idBloqueo);
    }

    // --- Estado operativo ---

    @GetMapping("/pendientes-preparacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'PERSONAL_SERVICIO')")
    public List<ApartamentoR> pendientesDePreparacion() {
        return apartamentos.pendientesDePreparacion().stream().map(ApartamentoR::de).toList();
    }

    @PutMapping("/{codigo}/estado-operativo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PERSONAL_SERVICIO')")
    public ApartamentoR cambiarEstado(@PathVariable String codigo, @Valid @RequestBody EstadoSolicitud s,
                                      @AuthenticationPrincipal Jwt jwt) {
        return ApartamentoR.de(apartamentos.cambiarEstadoOperativo(codigo, s.estado(), solicitante.de(jwt)));
    }

    // --- Novedades ---

    @PostMapping("/{codigo}/novedades")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'PERSONAL_SERVICIO')")
    public NovedadR registrarNovedad(@PathVariable String codigo, @Valid @RequestBody NovedadSolicitud s,
                                     @AuthenticationPrincipal Jwt jwt) {
        return NovedadR.de(apartamentos.registrarNovedad(codigo, s.descripcion(), s.gravedad(), solicitante.de(jwt)));
    }

    @GetMapping("/{codigo}/novedades")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'PERSONAL_SERVICIO')")
    public PaginaRespuesta<NovedadR> novedades(@PathVariable String codigo, @RequestParam(defaultValue = "0") int pagina) {
        return PaginaRespuesta.de(apartamentos.historialNovedades(codigo, pagina), NovedadR::de);
    }
}

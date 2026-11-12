package co.edu.uniquindio.sga.infrastructure.rest.canal;

import co.edu.uniquindio.sga.application.canal.CanalExternoServicio;
import co.edu.uniquindio.sga.application.reserva.DisponibilidadServicio;
import co.edu.uniquindio.sga.domain.canal.CanalVenta;
import co.edu.uniquindio.sga.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga.domain.canal.ResultadoReservaExterna;
import co.edu.uniquindio.sga.domain.reserva.Ocupante;
import co.edu.uniquindio.sga.domain.reserva.Titular;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.ReservaR;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Contrato del canal externo (10.3.1). No usa JWT: cada petición se autentica con el
 * código del canal y su credencial en los encabezados {@value #ENCABEZADO_CANAL} y
 * {@value #ENCABEZADO_CREDENCIAL}.
 */
@RestController
@RequestMapping("/api/canal")
public class CanalExternoControlador {

    public static final String ENCABEZADO_CANAL = "X-Canal";
    public static final String ENCABEZADO_CREDENCIAL = "X-Credencial-Canal";

    public record TitularExterno(@NotBlank String nombre, @NotBlank String documento, @Email String correo,
                                 String telefono, @NotNull @Past LocalDate fechaNacimiento) {
    }

    public record OcupanteExterno(@NotBlank String nombre, @NotNull @Past LocalDate fechaNacimiento) {
    }

    public record ReservaExternaSolicitud(@NotBlank @Size(max = 100) String idExterno,
                                          @NotBlank String codigoApartamento, @NotNull LocalDate entrada,
                                          @NotNull LocalDate salida, @NotNull LocalTime horaEstimadaLlegada,
                                          @NotNull @Valid TitularExterno titular,
                                          List<@Valid OcupanteExterno> acompanantes, @Min(0) int mascotas) {
    }

    public record OfertaR(String codigoApartamento, String nombre, int capacidad, int dormitorios,
                          boolean admiteMascotas, long noches, BigDecimal valorTotal) {
        static OfertaR de(DisponibilidadServicio.Resultado r) {
            return new OfertaR(r.apartamento().getCodigo(), r.apartamento().getNombre(), r.apartamento().getCapacidad(),
                    r.apartamento().getDormitorios(), r.apartamento().admiteMascotas(),
                    r.cotizacion().detalle().size(), Respuestas.pesos(r.cotizacion().valorTotal()));
        }
    }

    public record DisponibilidadR(String canal, LocalDate entrada, LocalDate salida, List<OfertaR> disponibles) {
    }

    public record ConflictoR(String id, String canal, String idExterno, String codigoApartamento, LocalDate entrada,
                             LocalDate salida, String titular, String motivo, String mensaje) {
        static ConflictoR de(ConflictoCanal c) {
            return new ConflictoR(c.id(), c.referencia().canal(), c.referencia().idExterno(), c.codigoApartamento(),
                    c.estancia().fechaEntrada(), c.estancia().fechaSalida(), c.titular(), c.motivo().name(),
                    c.motivo().mensaje());
        }
    }

    /**
     * @param resultado ACEPTADA, DUPLICADA, RECHAZADA_CONFLICTO, CANCELADA o YA_CANCELADA
     */
    public record RespuestaCanalR(String resultado, String mensaje, ReservaR reserva, ConflictoR conflicto) {
    }

    private final CanalExternoServicio servicio;

    public CanalExternoControlador(CanalExternoServicio servicio) {
        this.servicio = servicio;
    }

    /** Consulta de disponibilidad para un rango de fechas y una composición de grupo. */
    @GetMapping("/disponibilidad")
    public DisponibilidadR disponibilidad(@RequestHeader(value = ENCABEZADO_CANAL, required = false) String codigo,
                                          @RequestHeader(value = ENCABEZADO_CREDENCIAL, required = false) String credencial,
                                          @RequestParam LocalDate entrada, @RequestParam LocalDate salida,
                                          @RequestParam(required = false) Integer personas,
                                          @RequestParam(required = false) List<LocalDate> fechasNacimiento) {
        CanalVenta canal = servicio.autenticar(codigo, credencial);
        return new DisponibilidadR(canal.getCodigo(), entrada, salida,
                servicio.consultarDisponibilidad(canal, entrada, salida, fechasNacimiento, personas).stream()
                        .map(OfertaR::de).toList());
    }

    /**
     * 201: reserva creada. 200: el mensaje ya se había recibido (RN-19), se devuelve la misma
     * reserva. 409: colisiona con una reserva vigente; se rechaza y queda como conflicto (RN-18).
     */
    @PostMapping("/reservas")
    public ResponseEntity<RespuestaCanalR> crear(@RequestHeader(value = ENCABEZADO_CANAL, required = false) String codigo,
                                                 @RequestHeader(value = ENCABEZADO_CREDENCIAL, required = false) String credencial,
                                                 @Valid @RequestBody ReservaExternaSolicitud s) {
        CanalVenta canal = servicio.autenticar(codigo, credencial);
        TitularExterno t = s.titular();
        List<Ocupante> acompanantes = s.acompanantes() == null ? List.of()
                : s.acompanantes().stream().map(o -> new Ocupante(o.nombre(), o.fechaNacimiento())).toList();
        ResultadoReservaExterna resultado = servicio.crearReserva(canal, new CanalExternoServicio.SolicitudExterna(
                s.idExterno().trim(), s.codigoApartamento(), s.entrada(), s.salida(), s.horaEstimadaLlegada(),
                new Titular(t.nombre(), t.documento(), t.correo(), t.telefono(), t.fechaNacimiento()),
                acompanantes, s.mascotas()));
        return switch (resultado) {
            case ResultadoReservaExterna.Aceptada(var reserva) -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RespuestaCanalR("ACEPTADA", "Reserva creada", ReservaR.de(reserva), null));
            case ResultadoReservaExterna.Duplicada(var reserva) -> ResponseEntity.ok(new RespuestaCanalR("DUPLICADA",
                    "El mensaje ya se había recibido: no se creó otra reserva", ReservaR.de(reserva), null));
            case ResultadoReservaExterna.RechazadaPorConflicto(var conflicto) -> ResponseEntity
                    .status(HttpStatus.CONFLICT).body(new RespuestaCanalR("RECHAZADA_CONFLICTO",
                            conflicto.motivo().mensaje() + ". Quedó registrada para revisión del administrador.",
                            null, ConflictoR.de(conflicto)));
        };
    }

    @DeleteMapping("/reservas/{idExterno}")
    public RespuestaCanalR cancelar(@RequestHeader(value = ENCABEZADO_CANAL, required = false) String codigo,
                                    @RequestHeader(value = ENCABEZADO_CREDENCIAL, required = false) String credencial,
                                    @PathVariable String idExterno) {
        CanalVenta canal = servicio.autenticar(codigo, credencial);
        CanalExternoServicio.CancelacionExterna cancelacion = servicio.cancelarReserva(canal, idExterno);
        return cancelacion.repetida()
                ? new RespuestaCanalR("YA_CANCELADA", "La reserva ya estaba cancelada", ReservaR.de(cancelacion.reserva()), null)
                : new RespuestaCanalR("CANCELADA", "Reserva cancelada según la política congelada en ella",
                ReservaR.de(cancelacion.reserva()), null);
    }
}

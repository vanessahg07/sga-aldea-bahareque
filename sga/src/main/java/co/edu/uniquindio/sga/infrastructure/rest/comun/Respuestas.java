package co.edu.uniquindio.sga.infrastructure.rest.comun;

import co.edu.uniquindio.sga.application.apartamento.ApartamentoServicio;
import co.edu.uniquindio.sga.application.reserva.DisponibilidadServicio;
import co.edu.uniquindio.sga.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.apartamento.*;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.folio.*;
import co.edu.uniquindio.sga.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.reserva.*;
import co.edu.uniquindio.sga.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga.domain.tarifa.Temporada;
import co.edu.uniquindio.sga.domain.usuario.Rol;
import co.edu.uniquindio.sga.domain.usuario.Usuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Representaciones de salida de la API. Se construyen desde el dominio; nunca exponen entidades JPA. */
public final class Respuestas {

    private Respuestas() {
    }

    /** Valor en pesos para mostrar: sin ceros decimales sobrantes. */
    public static BigDecimal pesos(Dinero dinero) {
        BigDecimal valor = dinero.valor().stripTrailingZeros();
        return valor.scale() < 0 ? valor.setScale(0, RoundingMode.UNNECESSARY) : valor;
    }

    // --- Alojamiento ---

    public record ServicioR(String codigo, String nombre, String descripcion, boolean generaCargo, BigDecimal valor,
                            String unidad) {
    }

    public record ParametrosR(int umbralEdadFacturable, LocalTime horaEntrada, LocalTime horaSalida,
                              int horasPreparacion, int horasPlazoConfirmacion, LocalTime horaLimiteNoShow,
                              BigDecimal porcentajeAnticipo, int maxMascotas, BigDecimal cargoMascota,
                              int nochesDescuentoLargo, BigDecimal porcentajeDescuentoLargo,
                              int diasRecordatorioLlegada) {
        public static ParametrosR de(ParametrosAlojamiento p) {
            return new ParametrosR(p.umbralEdadFacturable(), p.horaEntrada(), p.horaSalida(), p.horasPreparacion(),
                    p.horasPlazoConfirmacion(), p.horaLimiteNoShow(), p.porcentajeAnticipo(), p.maxMascotas(),
                    pesos(p.cargoMascota()), p.nochesDescuentoLargo(), p.porcentajeDescuentoLargo(),
                    p.diasRecordatorioLlegada());
        }
    }

    public record AlojamientoR(String nombre, String descripcion, String ciudad, String direccion, BigDecimal latitud,
                               BigDecimal longitud, List<String> normas, List<ServicioR> servicios,
                               Set<String> mediosPago, ParametrosR parametros) {
        public static AlojamientoR de(Alojamiento a) {
            return new AlojamientoR(a.getNombre(), a.getDescripcion(), a.getCiudad(), a.getDireccion(),
                    a.getLatitud(), a.getLongitud(), a.getNormas(),
                    a.getServicios().stream().map(s -> new ServicioR(s.codigo(), s.nombre(), s.descripcion(),
                            s.generaCargo(), pesos(s.valor()), s.unidad())).toList(),
                    new TreeSet<>(a.getMediosPago().stream().map(MedioPago::nombre).toList()),
                    ParametrosR.de(a.getParametros()));
        }
    }

    // --- Apartamentos ---

    public record ImagenR(String url, boolean principal) {
    }

    public record ApartamentoR(String codigo, String nombre, String descripcion, int dormitorios, int capacidad,
                               Set<String> caracteristicas, boolean admiteMascotas, List<ImagenR> imagenes,
                               String imagenPrincipal, boolean activo, EstadoOperativo estadoOperativo) {
        public static ApartamentoR de(Apartamento a) {
            return new ApartamentoR(a.getCodigo(), a.getNombre(), a.getDescripcion(), a.getDormitorios(),
                    a.getCapacidad(), new TreeSet<>(a.getCaracteristicas()), a.admiteMascotas(),
                    a.getImagenes().stream().map(i -> new ImagenR(i.url(), i.principal())).toList(),
                    a.imagenPrincipal().map(ImagenApartamento::url).orElse(null), a.estaALaVenta(),
                    a.getEstadoOperativo());
        }
    }

    /** Tarjeta de resultado de búsqueda. */
    public record TarjetaR(String codigo, String nombre, String imagenPrincipal, int capacidad, int dormitorios,
                           Set<String> caracteristicas, long noches, BigDecimal valorEstimado, BigDecimal latitud,
                           BigDecimal longitud) {
        public static TarjetaR de(DisponibilidadServicio.Resultado r, Alojamiento alojamiento) {
            Apartamento a = r.apartamento();
            return new TarjetaR(a.getCodigo(), a.getNombre(), a.imagenPrincipal().map(ImagenApartamento::url).orElse(null),
                    a.getCapacidad(), a.getDormitorios(), new TreeSet<>(a.getCaracteristicas()),
                    r.cotizacion().detalle().size(), pesos(r.cotizacion().valorTotal()), alojamiento.getLatitud(),
                    alojamiento.getLongitud());
        }
    }

    public record NocheR(LocalDate fecha, ApartamentoServicio.EstadoNoche estado) {
        public static NocheR de(ApartamentoServicio.Noche n) {
            return new NocheR(n.fecha(), n.estado());
        }
    }

    public record BloqueoR(String id, String codigoApartamento, LocalDate desde, LocalDate hasta, String motivo) {
        public static BloqueoR de(Bloqueo b) {
            return new BloqueoR(b.id(), b.codigoApartamento(), b.rango().inicio(), b.rango().fin(), b.motivo());
        }
    }

    public record NovedadR(String id, String codigoApartamento, LocalDateTime fecha, String autor, String descripcion,
                           Novedad.Gravedad gravedad) {
        public static NovedadR de(Novedad n) {
            return new NovedadR(n.id(), n.codigoApartamento(), n.fecha(), n.autor(), n.descripcion(), n.gravedad());
        }
    }

    // --- Temporadas, tarifas y política ---

    public record RangoR(LocalDate inicio, LocalDate fin) {
    }

    public record TemporadaR(String nombre, boolean esBase, List<RangoR> rangos, int estanciaMinimaNoches) {
        public static TemporadaR de(Temporada t) {
            return new TemporadaR(t.nombre(), t.esBase(),
                    t.rangos().stream().map(r -> new RangoR(r.inicio(), r.fin())).toList(), t.estanciaMinimaNoches());
        }
    }

    public record TarifaR(String codigoApartamento, String temporada, BigDecimal valor, LocalDateTime vigenteDesde) {
        public static TarifaR de(Tarifa t) {
            return new TarifaR(t.codigoApartamento(), t.nombreTemporada(), pesos(t.valor()), t.vigenteDesde());
        }
    }

    public record TramoR(int diasAntelacionMinimos, BigDecimal porcentajeRetencion) {
    }

    public record PoliticaR(int version, LocalDateTime vigenteDesde, List<TramoR> tramos,
                            BigDecimal porcentajeRetencionNoShow) {
        public static PoliticaR de(PoliticaCancelacion p) {
            return new PoliticaR(p.version(), p.vigenteDesde(),
                    p.tramos().stream().map(t -> new TramoR(t.diasAntelacionMinimos(), t.porcentajeRetencion())).toList(),
                    p.porcentajeRetencionNoShow());
        }
    }

    // --- Cotización y reservas ---

    public record DetalleNocheR(LocalDate fecha, String temporada, BigDecimal tarifa, int ocupantesFacturables,
                                BigDecimal subtotal) {
    }

    public record CotizacionR(List<DetalleNocheR> detalle, BigDecimal subtotalNoches, BigDecimal descuento,
                              BigDecimal valorAlojamiento, BigDecimal valorMascotas, BigDecimal valorTotal) {
        public static CotizacionR de(CotizacionEstancia c) {
            return new CotizacionR(c.detalle().stream().map(n -> new DetalleNocheR(n.fecha(), n.temporada(),
                    pesos(n.tarifa()), n.ocupantesFacturables(), pesos(n.subtotal()))).toList(),
                    pesos(c.subtotalNoches()), pesos(c.descuento()), pesos(c.valorAlojamiento()),
                    pesos(c.valorMascotas()), pesos(c.valorTotal()));
        }
    }

    public record ResultadoCotizacionR(String codigoApartamento, LocalDate entrada, LocalDate salida, long noches,
                                       boolean disponible, String motivoNoDisponible, String mensajeNoDisponible,
                                       CotizacionR cotizacion) {
        public static ResultadoCotizacionR de(DisponibilidadServicio.Cotizacion c) {
            return new ResultadoCotizacionR(c.apartamento().getCodigo(), c.estancia().fechaEntrada(),
                    c.estancia().fechaSalida(), c.estancia().noches(), c.disponible(),
                    c.motivoNoDisponible() == null ? null : c.motivoNoDisponible().name(),
                    c.motivoNoDisponible() == null ? null : c.motivoNoDisponible().mensaje(),
                    CotizacionR.de(c.cotizacion()));
        }
    }

    public record TitularR(String nombre, String documento, String correo, String telefono, LocalDate fechaNacimiento,
                           boolean tieneCuenta) {
    }

    public record OcupanteR(String nombre, LocalDate fechaNacimiento) {
    }

    public record ReservaR(String id, EstadoReserva estado, Canal canal, String referenciaExterna,
                           String codigoApartamento, LocalDate entrada, LocalDate salida, long noches,
                           LocalTime horaEstimadaLlegada, TitularR titular, List<OcupanteR> acompanantes,
                           int totalOcupantes, int mascotas, LocalDateTime creadaEn, int versionPolitica,
                           CotizacionR cotizacion, BigDecimal saldo, boolean folioCerrado) {
        public static ReservaR de(Reserva r) {
            Titular t = r.getTitular();
            return new ReservaR(r.getId(), r.getEstado(), r.getCanal(),
                    r.getReferenciaExterna().map(x -> x.canal() + ":" + x.idExterno()).orElse(null),
                    r.getCodigoApartamento(), r.getEstancia().fechaEntrada(), r.getEstancia().fechaSalida(),
                    r.getEstancia().noches(), r.getHoraEstimadaLlegada().orElse(null),
                    new TitularR(t.nombre(), t.documento(), t.correo(), t.telefono(), t.fechaNacimiento(),
                            t.idCuenta() != null),
                    r.getAcompanantes().stream().map(o -> new OcupanteR(o.nombre(), o.fechaNacimiento())).toList(),
                    r.ocupantes().size(), r.getMascotas(), r.getCreadaEn(), r.getPolitica().version(),
                    CotizacionR.de(r.getCotizacion()), pesos(r.getFolio().saldo()), r.getFolio().isCerrado());
        }
    }

    /** Resumen para listados y calendarios. */
    public record ReservaResumenR(String id, EstadoReserva estado, Canal canal, String codigoApartamento,
                                  LocalDate entrada, LocalDate salida, String titular, int totalOcupantes,
                                  LocalTime horaEstimadaLlegada, BigDecimal valorTotal, BigDecimal saldo,
                                  LocalDateTime creadaEn) {
        public static ReservaResumenR de(Reserva r) {
            return new ReservaResumenR(r.getId(), r.getEstado(), r.getCanal(), r.getCodigoApartamento(),
                    r.getEstancia().fechaEntrada(), r.getEstancia().fechaSalida(), r.getTitular().nombre(),
                    r.ocupantes().size(), r.getHoraEstimadaLlegada().orElse(null),
                    pesos(r.getCotizacion().valorTotal()), pesos(r.getFolio().saldo()), r.getCreadaEn());
        }
    }

    public record ActualizacionApartamentoR(ApartamentoR apartamento, List<ReservaResumenR> reservasSobreCapacidad,
                                            String advertencia) {
        public static ActualizacionApartamentoR de(ApartamentoServicio.ResultadoActualizacion r) {
            List<ReservaResumenR> sobre = r.reservasSobreCapacidad().stream().map(ReservaResumenR::de).toList();
            return new ActualizacionApartamentoR(ApartamentoR.de(r.apartamento()), sobre, sobre.isEmpty() ? null
                    : "Hay " + sobre.size() + " reserva(s) activa(s) con más ocupantes que la nueva capacidad. "
                    + "Se conservan, pero deben revisarse.");
        }
    }

    // --- Folio ---

    public record CargoR(String id, TipoCargo tipo, BigDecimal valor, String descripcion, LocalDateTime fecha,
                         String idCargoRevertido) {
    }

    public record PagoR(String id, BigDecimal valor, String medio, LocalDateTime fecha, String idPagoRevertido) {
    }

    public record AutorizacionR(String autor, String motivo, LocalDateTime fecha) {
    }

    public record FolioR(String id, String idReserva, EstadoReserva estadoReserva, List<CargoR> cargos,
                         List<PagoR> pagos, BigDecimal totalCargos, BigDecimal totalPagos, BigDecimal saldo,
                         boolean cerrado, AutorizacionR autorizacionCierre) {
        public static FolioR de(Reserva r) {
            Folio f = r.getFolio();
            return new FolioR(f.getId(), r.getId(), r.getEstado(),
                    f.getCargos().stream().map(c -> new CargoR(c.id(), c.tipo(), pesos(c.valor()), c.descripcion(),
                            c.fecha(), c.idCargoRevertido())).toList(),
                    f.getPagos().stream().map(p -> new PagoR(p.id(), pesos(p.valor()), p.medio().nombre(), p.fecha(),
                            p.idPagoRevertido())).toList(),
                    pesos(f.totalCargos()), pesos(f.totalPagos()), pesos(f.saldo()), f.isCerrado(),
                    f.getAutorizacionCierre().map(a -> new AutorizacionR(a.autor(), a.motivo(), a.fecha())).orElse(null));
        }
    }

    // --- Usuarios ---

    public record UsuarioR(String id, String nombre, String correo, String telefono, String documento,
                           LocalDate fechaNacimiento, String fotoUrl, Rol rol) {
        public static UsuarioR de(Usuario u) {
            return new UsuarioR(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getDocumento(),
                    u.getFechaNacimiento(), u.getFotoUrl(), u.getRol());
        }
    }

    public record SesionR(String token, String tipo, java.time.Instant expiraEn, UsuarioR usuario) {
    }

    public record MensajeR(String mensaje) {
    }
}

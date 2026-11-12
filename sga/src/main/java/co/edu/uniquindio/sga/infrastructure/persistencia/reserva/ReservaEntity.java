package co.edu.uniquindio.sga.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga.domain.folio.TipoCargo;
import co.edu.uniquindio.sga.domain.reserva.Canal;
import co.edu.uniquindio.sga.domain.reserva.EstadoReserva;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reserva con su cotización congelada y su folio. La combinación canal externo +
 * identificador externo es única (RN-19).
 */
@Entity
@Table(name = "reserva",
        uniqueConstraints = @UniqueConstraint(name = "uk_reserva_referencia_externa",
                columnNames = {"canalExterno", "idExterno"}),
        indexes = {@Index(columnList = "codigoApartamento, estado"), @Index(columnList = "titularIdCuenta")})
@Getter
@Setter
@NoArgsConstructor
public class ReservaEntity {

    private static final int PRECISION = 17;
    private static final int ESCALA = 4;

    @Id
    private String id;

    // Titular
    private String titularNombre;
    private String titularDocumento;
    private String titularCorreo;
    private String titularTelefono;
    private LocalDate titularFechaNacimiento;
    private String titularIdCuenta;

    @ElementCollection
    @CollectionTable(name = "reserva_acompanante", joinColumns = @JoinColumn(name = "id_reserva"))
    @OrderColumn(name = "orden")
    private List<Acompanante> acompanantes = new ArrayList<>();

    private String codigoApartamento;
    private LocalDate entrada;
    private LocalDate salida;
    private LocalTime horaEstimadaLlegada;
    @Enumerated(EnumType.STRING)
    private Canal canal;
    private String canalExterno;
    private String idExterno;
    private int mascotas;
    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;
    private LocalDateTime creadaEn;
    private int versionPolitica;

    // Cotización congelada
    @Column(precision = PRECISION, scale = ESCALA)
    private BigDecimal subtotalNoches;
    @Column(precision = PRECISION, scale = ESCALA)
    private BigDecimal descuento;
    @Column(precision = PRECISION, scale = ESCALA)
    private BigDecimal valorAlojamiento;
    @Column(precision = PRECISION, scale = ESCALA)
    private BigDecimal valorMascotas;

    @ElementCollection
    @CollectionTable(name = "reserva_detalle_noche", joinColumns = @JoinColumn(name = "id_reserva"))
    @OrderColumn(name = "orden")
    private List<DetalleNoche> detalle = new ArrayList<>();

    // Folio
    private String folioId;
    private boolean folioCerrado;
    private String autorizacionAutor;
    @Column(length = 1000)
    private String autorizacionMotivo;
    private LocalDateTime autorizacionFecha;

    @ElementCollection
    @CollectionTable(name = "folio_cargo", joinColumns = @JoinColumn(name = "id_reserva"))
    @OrderColumn(name = "orden")
    private List<Cargo> cargos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "folio_pago", joinColumns = @JoinColumn(name = "id_reserva"))
    @OrderColumn(name = "orden")
    private List<Pago> pagos = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Acompanante {
        private String nombre;
        private LocalDate fechaNacimiento;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleNoche {
        private LocalDate fecha;
        private String temporada;
        @Column(precision = PRECISION, scale = ESCALA)
        private BigDecimal tarifa;
        private int ocupantesFacturables;
        @Column(precision = PRECISION, scale = ESCALA)
        private BigDecimal subtotal;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cargo {
        private String idCargo;
        @Enumerated(EnumType.STRING)
        private TipoCargo tipo;
        @Column(precision = PRECISION, scale = ESCALA)
        private BigDecimal valor;
        @Column(length = 1000)
        private String descripcion;
        private LocalDateTime fecha;
        private String idCargoRevertido;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pago {
        private String idPago;
        @Column(precision = PRECISION, scale = ESCALA)
        private BigDecimal valor;
        private String medio;
        private LocalDateTime fecha;
        private String idPagoRevertido;
    }
}

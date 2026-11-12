package co.edu.uniquindio.sga.infrastructure.persistencia.alojamiento;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "alojamiento")
@Getter
@Setter
@NoArgsConstructor
public class AlojamientoEntity {

    /** El sistema administra un único alojamiento. */
    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    private String codigo;
    private String nombre;
    @Column(length = 2000)
    private String descripcion;
    private String ciudad;
    private String direccion;
    @Column(precision = 9, scale = 6)
    private BigDecimal latitud;
    @Column(precision = 9, scale = 6)
    private BigDecimal longitud;

    @ElementCollection
    @CollectionTable(name = "alojamiento_norma", joinColumns = @JoinColumn(name = "alojamiento_id"))
    @OrderColumn(name = "orden")
    @Column(name = "norma", length = 500)
    private List<String> normas = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "alojamiento_servicio", joinColumns = @JoinColumn(name = "alojamiento_id"))
    @OrderColumn(name = "orden")
    private List<Servicio> servicios = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "alojamiento_medio_pago", joinColumns = @JoinColumn(name = "alojamiento_id"))
    @Column(name = "medio")
    private Set<String> mediosPago = new HashSet<>();

    // Parámetros configurables (Ficha, A.4)
    private int umbralEdadFacturable;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;
    private int horasPreparacion;
    private int horasPlazoConfirmacion;
    private LocalTime horaLimiteNoShow;
    @Column(precision = 5, scale = 2)
    private BigDecimal porcentajeAnticipo;
    private int maxMascotas;
    @Column(precision = 15, scale = 2)
    private BigDecimal cargoMascota;
    private int nochesDescuentoLargo;
    @Column(precision = 5, scale = 2)
    private BigDecimal porcentajeDescuentoLargo;
    /** Nulo en bases creadas antes del Corte 4: se toma el valor por defecto de la Ficha. */
    private Integer diasRecordatorioLlegada;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Servicio {
        private String codigo;
        private String nombre;
        @Column(length = 1000)
        private String descripcion;
        private boolean generaCargo;
        @Column(precision = 15, scale = 2)
        private BigDecimal valor;
        private String unidad;
    }
}

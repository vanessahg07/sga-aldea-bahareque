package co.edu.uniquindio.sga.infrastructure.persistencia.tarifa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Cada versión de la política se conserva; publicar una nueva agrega una fila. */
@Entity
@Table(name = "politica_cancelacion")
@Getter
@Setter
@NoArgsConstructor
public class PoliticaEntity {

    @Id
    private Integer version;
    private LocalDateTime vigenteDesde;
    @Column(precision = 5, scale = 2)
    private BigDecimal porcentajeRetencionNoShow;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "politica_tramo", joinColumns = @JoinColumn(name = "version_politica"))
    @OrderColumn(name = "orden")
    private List<Tramo> tramos = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tramo {
        private int diasAntelacionMinimos;
        @Column(precision = 5, scale = 2)
        private BigDecimal porcentajeRetencion;
    }
}

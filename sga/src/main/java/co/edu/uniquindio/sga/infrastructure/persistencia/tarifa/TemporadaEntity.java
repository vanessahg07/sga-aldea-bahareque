package co.edu.uniquindio.sga.infrastructure.persistencia.tarifa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "temporada")
@Getter
@Setter
@NoArgsConstructor
public class TemporadaEntity {

    @Id
    private String nombre;
    private boolean esBase;
    private int estanciaMinimaNoches;
    /** Retirar una temporada la deja inactiva; nunca se borra. */
    private boolean activa;

    @ElementCollection
    @CollectionTable(name = "temporada_rango", joinColumns = @JoinColumn(name = "temporada"))
    @OrderColumn(name = "orden")
    private List<Rango> rangos = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rango {
        private LocalDate inicio;
        private LocalDate fin;
    }
}

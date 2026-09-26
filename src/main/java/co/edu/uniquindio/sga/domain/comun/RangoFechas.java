package co.edu.uniquindio.sga.domain.comun;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Rango de noches cerrado al inicio y abierto al final: {@code [inicio, fin)}.
 * La noche de la fecha {@code fin} no pertenece al rango.
 */
public record RangoFechas(LocalDate inicio, LocalDate fin) {

    public RangoFechas {
        ExcepcionNegocio.exigir(inicio != null && fin != null && fin.isAfter(inicio),
                CodigoError.ESTANCIA_INVALIDA);
    }

    public long noches() {
        return ChronoUnit.DAYS.between(inicio, fin);
    }

    /** Dos rangos se solapan si comparten al menos una noche. */
    public boolean seSolapaCon(RangoFechas otro) {
        return inicio.isBefore(otro.fin) && otro.inicio.isBefore(fin);
    }

    /** Indica si la noche de la fecha dada pertenece al rango. */
    public boolean contiene(LocalDate noche) {
        return !noche.isBefore(inicio) && noche.isBefore(fin);
    }

    public List<LocalDate> listaNoches() {
        return inicio.datesUntil(fin).toList();
    }
}

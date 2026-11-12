package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.comun.RangoFechas;

import java.time.LocalDate;
import java.util.List;

/**
 * Rango continuo de noches que ocupa una reserva: {@code [fechaEntrada, fechaSalida)}.
 * RN-03: la fecha de salida es posterior a la de entrada, así que toda estancia
 * tiene al menos una noche.
 */
public record Estancia(LocalDate fechaEntrada, LocalDate fechaSalida) {

    public Estancia {
        new RangoFechas(fechaEntrada, fechaSalida);
    }

    public RangoFechas rango() {
        return new RangoFechas(fechaEntrada, fechaSalida);
    }

    public long noches() {
        return rango().noches();
    }

    public List<LocalDate> listaNoches() {
        return rango().listaNoches();
    }

    /** RN-01: dos estancias se solapan si comparten al menos una noche. */
    public boolean seSolapaCon(Estancia otra) {
        return rango().seSolapaCon(otra.rango());
    }
}

package co.edu.uniquindio.sga.domain.reporte;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Periodo de un reporte con ambas fechas incluidas. Abarca las noches desde {@code desde}
 * hasta {@code hasta}: un periodo del 1 al 30 de junio tiene 30 noches.
 */
public record Periodo(LocalDate desde, LocalDate hasta) {

    public static final int MAXIMO_DIAS = 366;

    public Periodo {
        ExcepcionNegocio.exigir(desde != null && hasta != null && !hasta.isBefore(desde)
                && ChronoUnit.DAYS.between(desde, hasta) < MAXIMO_DIAS, CodigoError.PERIODO_INVALIDO);
    }

    public RangoFechas noches() {
        return new RangoFechas(desde, hasta.plusDays(1));
    }

    public long cantidadNoches() {
        return noches().noches();
    }

    public boolean contiene(LocalDate fecha) {
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }
}

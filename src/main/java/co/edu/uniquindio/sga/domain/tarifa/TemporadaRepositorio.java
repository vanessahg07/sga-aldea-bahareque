package co.edu.uniquindio.sga.domain.tarifa;

/** Puerto de persistencia del calendario de temporadas. */
public interface TemporadaRepositorio {

    CalendarioTemporadas calendario();

    /** Guarda el calendario completo; las temporadas que ya no estén quedan inactivas. */
    void guardar(CalendarioTemporadas calendario);
}

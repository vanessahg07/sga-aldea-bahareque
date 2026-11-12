package co.edu.uniquindio.sga.domain.reporte;

import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.reserva.Canal;
import co.edu.uniquindio.sga.domain.reserva.EstadoReserva;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Resultados de los reportes del alojamiento. Todos se calculan sobre un {@link Periodo}. */
public final class Reportes {

    private Reportes() {
    }

    /**
     * Ocupación: noches vendidas / (apartamentos activos × noches del periodo).
     *
     * @param porcentaje porcentaje de 0 a 100 con dos decimales
     */
    public record Ocupacion(Periodo periodo, int apartamentosActivos, long nochesDisponibles, long nochesVendidas,
                            BigDecimal porcentaje, List<OcupacionApartamento> porApartamento) {
    }

    public record OcupacionApartamento(String codigoApartamento, long nochesVendidas, BigDecimal porcentaje) {
    }

    /** Ingresos: pagos registrados en el periodo por medio de pago. Los cargos no pagados no son ingreso. */
    public record Ingresos(Periodo periodo, List<IngresoPorMedio> porMedio, Dinero total, int cantidadPagos) {
    }

    /** @param pagos cantidad de movimientos de pago, incluidos los reversos */
    public record IngresoPorMedio(String medio, Dinero total, int pagos) {
    }

    /** Reservas creadas en el periodo por canal de origen, incluidas las canceladas y los no-show. */
    public record ReservasPorCanal(Periodo periodo, List<FilaCanal> canales, long total) {
    }

    public record FilaCanal(Canal canal, Map<EstadoReserva, Long> porEstado, long total) {
    }

    /**
     * Indicadores propios del alojamiento (L-19).
     *
     * @param tarifaPromedioNoche      ingreso por alojamiento de las noches vendidas / noches vendidas (ADR)
     * @param ingresoPorNocheDisponible ingreso por alojamiento / noches disponibles (RevPAR)
     * @param tasaNoShow               no-show / reservas en firme cuya entrada cae en el periodo, en %
     * @param tasaCancelacion          canceladas / reservas creadas en el periodo, en %
     * @param antelacionPromedioDias   días promedio entre la creación de la reserva y la entrada
     */
    public record Indicadores(Periodo periodo, Dinero ingresoAlojamiento, Dinero tarifaPromedioNoche,
                              Dinero ingresoPorNocheDisponible, BigDecimal tasaNoShow, BigDecimal tasaCancelacion,
                              BigDecimal antelacionPromedioDias) {
    }
}

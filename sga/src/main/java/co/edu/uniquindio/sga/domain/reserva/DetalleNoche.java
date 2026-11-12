package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.comun.Dinero;

import java.time.LocalDate;

/** Línea del desglose de una cotización: una noche con su temporada y su tarifa. */
public record DetalleNoche(LocalDate fecha, String temporada, Dinero tarifa, int ocupantesFacturables,
                           Dinero subtotal) {
}

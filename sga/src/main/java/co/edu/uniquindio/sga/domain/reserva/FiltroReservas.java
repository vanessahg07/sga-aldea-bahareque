package co.edu.uniquindio.sga.domain.reserva;

import java.time.LocalDate;

/**
 * Criterios para listar reservas. Todos son opcionales (null = sin filtro).
 *
 * @param desde    reservas cuya estancia termina después de esta fecha
 * @param hasta    reservas cuya estancia empieza antes de esta fecha
 * @param titular  texto contenido en el nombre o el documento del titular
 * @param idCuenta cuenta del titular (para "mis reservas")
 */
public record FiltroReservas(LocalDate desde, LocalDate hasta, EstadoReserva estado, String codigoApartamento,
                             Canal canal, String titular, String idCuenta) {

    public static FiltroReservas deCuenta(String idCuenta) {
        return new FiltroReservas(null, null, null, null, null, null, idCuenta);
    }
}

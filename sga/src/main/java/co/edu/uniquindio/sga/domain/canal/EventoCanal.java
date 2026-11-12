package co.edu.uniquindio.sga.domain.canal;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entrada de la bitácora de eventos intercambiados con los canales externos.
 *
 * @param codigoCanal canal que originó el evento (tal como se presentó, aunque no exista)
 * @param idExterno   identificador propio del canal, cuando el evento se refiere a una reserva
 * @param idReserva   reserva del SGA involucrada, si la hay
 * @param detalle     resultado legible para el administrador
 */
public record EventoCanal(String id, String codigoCanal, TipoEventoCanal tipo, String idExterno, String idReserva,
                          String detalle, LocalDateTime fecha) {

    public EventoCanal {
        ExcepcionNegocio.exigir(tipo != null && fecha != null, CodigoError.CANAL_INVALIDO, "evento sin tipo o fecha");
    }

    public static EventoCanal de(String codigoCanal, TipoEventoCanal tipo, String idExterno, String idReserva,
                                 String detalle, LocalDateTime fecha) {
        return new EventoCanal(UUID.randomUUID().toString(), codigoCanal, tipo, idExterno, idReserva, detalle, fecha);
    }
}

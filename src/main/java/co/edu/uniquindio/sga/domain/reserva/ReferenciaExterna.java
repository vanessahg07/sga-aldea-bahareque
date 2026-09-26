package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

/**
 * Identificación de una reserva en un canal externo. RN-19: la combinación
 * canal + identificador externo es única.
 */
public record ReferenciaExterna(String canal, String idExterno) {

    public ReferenciaExterna {
        ExcepcionNegocio.exigir(canal != null && !canal.isBlank() && idExterno != null && !idExterno.isBlank(),
                CodigoError.RESERVA_EXTERNA_INVALIDA);
    }
}

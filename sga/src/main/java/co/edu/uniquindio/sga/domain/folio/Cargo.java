package co.edu.uniquindio.sga.domain.folio;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDateTime;

/**
 * Movimiento que suma al folio. Es inmutable: una corrección se hace con otro cargo
 * de valor inverso que referencia al original en {@code idCargoRevertido}.
 * Un ajuste por modificación puede ser negativo.
 */
public record Cargo(String id, TipoCargo tipo, Dinero valor, String descripcion, LocalDateTime fecha,
                    String idCargoRevertido) {

    public Cargo {
        ExcepcionNegocio.exigir(id != null && tipo != null && valor != null && fecha != null,
                CodigoError.CARGO_INVALIDO);
        ExcepcionNegocio.exigir(descripcion != null && !descripcion.isBlank(), CodigoError.CARGO_INVALIDO,
                "descripción");
    }

    public boolean esReverso() {
        return idCargoRevertido != null;
    }
}

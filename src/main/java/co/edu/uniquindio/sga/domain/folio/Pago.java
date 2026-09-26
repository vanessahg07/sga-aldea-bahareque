package co.edu.uniquindio.sga.domain.folio;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDateTime;

/**
 * Abono registrado contra un folio. Todo pago tiene medio y fecha. Es inmutable:
 * su corrección es un pago inverso (valor negativo) que referencia al original.
 */
public record Pago(String id, Dinero valor, MedioPago medio, LocalDateTime fecha, String idPagoRevertido) {

    public Pago {
        ExcepcionNegocio.exigir(id != null && valor != null && medio != null && fecha != null,
                CodigoError.PAGO_INVALIDO);
        ExcepcionNegocio.exigir(idPagoRevertido != null ? valor.esNegativo() : valor.esPositivo(),
                CodigoError.PAGO_INVALIDO, "valor");
    }

    public boolean esReverso() {
        return idPagoRevertido != null;
    }
}

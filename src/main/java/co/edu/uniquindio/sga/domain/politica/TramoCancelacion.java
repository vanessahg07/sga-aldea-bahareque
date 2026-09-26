package co.edu.uniquindio.sga.domain.politica;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.math.BigDecimal;

/**
 * Tramo de antelación de la política de cancelación: si se cancela con al menos
 * {@code diasAntelacionMinimos} días antes de la entrada, se retiene el porcentaje indicado.
 */
public record TramoCancelacion(int diasAntelacionMinimos, BigDecimal porcentajeRetencion) {

    public TramoCancelacion {
        ExcepcionNegocio.exigir(diasAntelacionMinimos >= 0, CodigoError.POLITICA_INVALIDA, "antelación");
        ExcepcionNegocio.exigir(porcentajeRetencion != null && porcentajeRetencion.signum() >= 0
                        && porcentajeRetencion.compareTo(BigDecimal.valueOf(100)) <= 0,
                CodigoError.POLITICA_INVALIDA, "porcentaje de retención");
    }
}

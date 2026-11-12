package co.edu.uniquindio.sga.domain.tarifa;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDateTime;

/**
 * Valor por ocupante facturable, por noche, de un apartamento en una temporada.
 * Una nueva tarifa no reemplaza a la anterior: se agrega con una fecha de vigencia
 * posterior, y así se conserva el histórico.
 */
public record Tarifa(String codigoApartamento, String nombreTemporada, Dinero valor, LocalDateTime vigenteDesde) {

    public Tarifa {
        ExcepcionNegocio.exigir(codigoApartamento != null && nombreTemporada != null && vigenteDesde != null,
                CodigoError.TARIFA_INVALIDA);
        ExcepcionNegocio.exigir(valor != null && valor.esPositivo(), CodigoError.TARIFA_INVALIDA);
    }
}

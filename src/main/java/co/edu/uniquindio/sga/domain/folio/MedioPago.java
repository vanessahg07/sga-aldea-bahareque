package co.edu.uniquindio.sga.domain.folio;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

/**
 * Medio con el que se registra un pago. Los medios aceptados son configuración del
 * alojamiento (Ficha A.5), por eso no es un enumerado cerrado.
 */
public record MedioPago(String nombre) {

    public MedioPago {
        ExcepcionNegocio.exigir(nombre != null && !nombre.isBlank(), CodigoError.PAGO_INVALIDO, "medio de pago");
        nombre = nombre.trim().toUpperCase();
    }
}

package co.edu.uniquindio.sga.domain.alojamiento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

/**
 * Servicio que el alojamiento ofrece además del alojamiento, con o sin cargo.
 *
 * @param unidad cómo se cobra el valor, por ejemplo "por persona por día" o "por trayecto"
 */
public record ServicioAdicional(String codigo, String nombre, String descripcion, boolean generaCargo, Dinero valor,
                                String unidad) {

    public ServicioAdicional {
        ExcepcionNegocio.exigir(codigo != null && !codigo.isBlank(), CodigoError.ALOJAMIENTO_INVALIDO, "código del servicio");
        ExcepcionNegocio.exigir(nombre != null && !nombre.isBlank(), CodigoError.ALOJAMIENTO_INVALIDO, "nombre del servicio");
        valor = valor == null ? Dinero.CERO : valor;
        ExcepcionNegocio.exigir(generaCargo ? valor.esPositivo() : valor.esCero(), CodigoError.ALOJAMIENTO_INVALIDO,
                "un servicio con cargo necesita valor positivo y uno sin cargo, valor cero");
        codigo = codigo.trim().toUpperCase();
    }

    /** Valor a cargar por la cantidad de unidades consumidas. */
    public Dinero valorPor(int cantidad) {
        ExcepcionNegocio.exigir(cantidad >= 1, CodigoError.CARGO_INVALIDO, "cantidad");
        return valor.por(cantidad);
    }
}

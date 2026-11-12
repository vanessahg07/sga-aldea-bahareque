package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDate;
import java.time.Period;

/**
 * Persona incluida en una reserva. Se registra su fecha de nacimiento; la edad se
 * calcula, nunca se almacena.
 */
public record Ocupante(String nombre, LocalDate fechaNacimiento) {

    public Ocupante {
        ExcepcionNegocio.exigir(nombre != null && !nombre.isBlank(), CodigoError.OCUPANTE_INVALIDO, "nombre");
        ExcepcionNegocio.exigir(fechaNacimiento != null, CodigoError.OCUPANTE_INVALIDO, "fecha de nacimiento");
    }

    public int edadEn(LocalDate fecha) {
        return Period.between(fechaNacimiento, fecha).getYears();
    }

    /**
     * RN-06: es facturable si a la fecha de entrada alcanza el umbral. Cumplir años
     * durante la estancia no cambia su condición.
     */
    public boolean esFacturable(LocalDate fechaEntrada, int umbralEdadFacturable) {
        return edadEn(fechaEntrada) >= umbralEdadFacturable;
    }
}

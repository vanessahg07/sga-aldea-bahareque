package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Responsable de la reserva y de su pago. Es siempre un ocupante facturable.
 * Existe aunque nunca use el sistema: puede o no tener una cuenta asociada, y el
 * dominio solo conserva la referencia a ella, sin depender del concepto de usuario.
 *
 * @param idCuenta identificador de la cuenta del titular, o null si no tiene
 */
public record Titular(String nombre, String documento, String correo, String telefono, LocalDate fechaNacimiento,
                      String idCuenta) {

    public Titular {
        ExcepcionNegocio.exigir(nombre != null && !nombre.isBlank(), CodigoError.TITULAR_INVALIDO, "nombre");
        ExcepcionNegocio.exigir(documento != null && !documento.isBlank(), CodigoError.TITULAR_INVALIDO, "documento");
        ExcepcionNegocio.exigir(fechaNacimiento != null, CodigoError.TITULAR_INVALIDO, "fecha de nacimiento");
    }

    /** Titular sin cuenta en el sistema (reserva directa o externa). */
    public Titular(String nombre, String documento, String correo, String telefono, LocalDate fechaNacimiento) {
        this(nombre, documento, correo, telefono, fechaNacimiento, null);
    }

    public Ocupante comoOcupante() {
        return new Ocupante(nombre, fechaNacimiento);
    }

    public Optional<String> cuenta() {
        return Optional.ofNullable(idCuenta);
    }

    public boolean perteneceA(String idCuentaConsultada) {
        return idCuenta != null && idCuenta.equals(idCuentaConsultada);
    }
}

package co.edu.uniquindio.sga.application.comun;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.reserva.Reserva;
import co.edu.uniquindio.sga.domain.usuario.Rol;

/**
 * Quién ejecuta un caso de uso. La autenticación ocurre en la infraestructura; aquí solo
 * llega la identidad ya verificada para aplicar las restricciones por propietario.
 */
public record Solicitante(String idUsuario, String nombre, Rol rol) {

    public boolean esHuesped() {
        return rol == Rol.HUESPED;
    }

    public boolean esAdministrador() {
        return rol == Rol.ADMINISTRADOR;
    }

    /** Un huésped solo accede a las reservas de las que es titular; el personal interno accede a todas. */
    public void exigirAcceso(Reserva reserva) {
        ExcepcionNegocio.exigir(!esHuesped() || reserva.getTitular().perteneceA(idUsuario), CodigoError.ACCESO_DENEGADO);
    }

    public void exigirAdministrador() {
        ExcepcionNegocio.exigir(esAdministrador(), CodigoError.ACCESO_DENEGADO);
    }
}

package co.edu.uniquindio.sga.domain.usuario;

import java.util.Optional;

/** Puerto de persistencia de códigos de recuperación. */
public interface CodigoRecuperacionRepositorio {

    void guardar(CodigoRecuperacion codigo);

    /** El código más reciente emitido para el correo, sin importar si está vigente. */
    Optional<CodigoRecuperacion> ultimoDe(String correo);
}

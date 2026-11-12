package co.edu.uniquindio.sga.domain.alojamiento;

import java.util.Optional;

/** Puerto de persistencia del alojamiento. El sistema administra uno solo. */
public interface AlojamientoRepositorio {

    Optional<Alojamiento> obtener();

    void guardar(Alojamiento alojamiento);
}

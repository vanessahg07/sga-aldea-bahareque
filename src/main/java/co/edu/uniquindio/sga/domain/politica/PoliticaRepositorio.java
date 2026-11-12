package co.edu.uniquindio.sga.domain.politica;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de la política de cancelación. Todas las versiones se conservan. */
public interface PoliticaRepositorio {

    Optional<PoliticaCancelacion> vigente();

    Optional<PoliticaCancelacion> porVersion(int version);

    List<PoliticaCancelacion> todas();

    void guardar(PoliticaCancelacion politica);
}

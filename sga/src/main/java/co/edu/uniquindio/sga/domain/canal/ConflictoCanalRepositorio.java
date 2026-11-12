package co.edu.uniquindio.sga.domain.canal;

import co.edu.uniquindio.sga.domain.comun.Pagina;

import java.util.Optional;

/** Puerto de persistencia de los conflictos de canal (RN-18). */
public interface ConflictoCanalRepositorio {

    void guardar(ConflictoCanal conflicto);

    Optional<ConflictoCanal> buscar(String id);

    /** Conflictos del más reciente al más antiguo; opcionalmente solo los pendientes de revisión. */
    Pagina<ConflictoCanal> listar(boolean soloPendientes, int pagina);
}

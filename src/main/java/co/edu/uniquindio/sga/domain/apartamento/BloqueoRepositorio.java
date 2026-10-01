package co.edu.uniquindio.sga.domain.apartamento;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de bloqueos. Levantar un bloqueo es una eliminación lógica. */
public interface BloqueoRepositorio {

    void guardar(Bloqueo bloqueo);

    Optional<Bloqueo> buscarVigente(String id);

    List<Bloqueo> vigentesDe(String codigoApartamento);

    void levantar(String id);
}

package co.edu.uniquindio.sga.domain.apartamento;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de apartamentos. Los listados ignoran los eliminados. */
public interface ApartamentoRepositorio {

    void guardar(Apartamento apartamento);

    Optional<Apartamento> buscar(String codigo);

    /**
     * Busca el apartamento reservándolo en exclusiva hasta el fin de la transacción.
     * Evita que dos reservas simultáneas vendan la misma noche (RN-01).
     */
    Optional<Apartamento> buscarParaActualizar(String codigo);

    boolean existe(String codigo);

    List<Apartamento> listarNoEliminados();
}

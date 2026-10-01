package co.edu.uniquindio.sga.domain.apartamento;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de apartamentos. Los listados ignoran los eliminados. */
public interface ApartamentoRepositorio {

    void guardar(Apartamento apartamento);

    Optional<Apartamento> buscar(String codigo);

    boolean existe(String codigo);

    List<Apartamento> listarNoEliminados();
}

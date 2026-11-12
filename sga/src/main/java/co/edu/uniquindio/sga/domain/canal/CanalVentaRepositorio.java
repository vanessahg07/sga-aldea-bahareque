package co.edu.uniquindio.sga.domain.canal;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de los canales de venta. */
public interface CanalVentaRepositorio {

    void guardar(CanalVenta canal);

    Optional<CanalVenta> buscar(String codigo);

    boolean existe(String codigo);

    List<CanalVenta> listar();
}

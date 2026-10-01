package co.edu.uniquindio.sga.domain.apartamento;

import co.edu.uniquindio.sga.domain.comun.Pagina;

/** Puerto de persistencia de novedades. */
public interface NovedadRepositorio {

    void guardar(Novedad novedad);

    /** Historial de novedades de un apartamento, de la más reciente a la más antigua. */
    Pagina<Novedad> deApartamento(String codigoApartamento, int pagina);
}

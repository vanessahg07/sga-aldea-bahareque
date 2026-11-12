package co.edu.uniquindio.sga.domain.canal;

import co.edu.uniquindio.sga.domain.comun.Pagina;

/**
 * Puerto de la bitácora de canales. Un evento se conserva aunque la operación que lo
 * originó sea rechazada: la bitácora registra lo que se intercambió, no solo lo exitoso.
 */
public interface BitacoraCanalRepositorio {

    void registrar(EventoCanal evento);

    /** Eventos del más reciente al más antiguo, opcionalmente de un solo canal. */
    Pagina<EventoCanal> listar(String codigoCanal, int pagina);
}

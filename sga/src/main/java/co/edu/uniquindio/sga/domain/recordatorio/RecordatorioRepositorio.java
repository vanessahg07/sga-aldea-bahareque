package co.edu.uniquindio.sga.domain.recordatorio;

import co.edu.uniquindio.sga.domain.comun.Pagina;

/** Puerto de persistencia de los recordatorios enviados. */
public interface RecordatorioRepositorio {

    void guardar(Recordatorio recordatorio);

    boolean fueEnviado(String idReserva, TipoRecordatorio tipo);

    /** Recordatorios del más reciente al más antiguo. */
    Pagina<Recordatorio> listar(int pagina);
}

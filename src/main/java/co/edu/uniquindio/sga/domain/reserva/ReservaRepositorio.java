package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.comun.Pagina;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de reservas con su folio. */
public interface ReservaRepositorio {

    void guardar(Reserva reserva);

    Optional<Reserva> buscar(String id);

    /** RN-19: la combinación canal + identificador externo es única. */
    Optional<Reserva> buscarPorReferenciaExterna(ReferenciaExterna referencia);

    /** Reservas activas del apartamento, para verificar disponibilidad. */
    List<Reserva> activasDe(String codigoApartamento);

    /** Reservas activas de todos los apartamentos cuya estancia toca el rango [desde, hasta). */
    List<Reserva> activasEnRango(LocalDate desde, LocalDate hasta);

    /** Listado filtrado, de la más reciente a la más antigua. */
    Pagina<Reserva> listar(FiltroReservas filtro, int pagina);

    /** RN-21: reservas PENDIENTE, para vencer las que superan el plazo de confirmación. */
    List<Reserva> pendientes();
}

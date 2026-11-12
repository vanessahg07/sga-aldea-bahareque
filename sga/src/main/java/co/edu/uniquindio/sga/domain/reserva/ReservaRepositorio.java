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

    List<Reserva> pendientes();

    List<Reserva> conEntradaEl(LocalDate fecha);

    List<Reserva> conSalidaEl(LocalDate fecha);

    boolean existenActivasDeCuenta(String idCuenta);

    /**
     * Reservas que intervienen en los reportes de un periodo (fechas incluidas): las que
     * tienen noches en él, las creadas en él y las que tienen pagos en él, en cualquier estado.
     */
    List<Reserva> paraReportes(LocalDate desde, LocalDate hasta);
}

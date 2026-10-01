package co.edu.uniquindio.sga.infrastructure.persistencia.memoria;

import co.edu.uniquindio.sga.domain.comun.Pagina;
import co.edu.uniquindio.sga.domain.reserva.FiltroReservas;
import co.edu.uniquindio.sga.domain.reserva.ReferenciaExterna;
import co.edu.uniquindio.sga.domain.reserva.Reserva;
import co.edu.uniquindio.sga.domain.reserva.ReservaRepositorio;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static co.edu.uniquindio.sga.domain.reserva.EstadoReserva.PENDIENTE;

/**
 * Implementación en memoria del puerto {@link ReservaRepositorio}, indexada por el
 * identificador de la reserva. Sirve para probar los casos de uso sin base de datos;
 * el adaptador JPA la reemplaza sin cambiar el dominio.
 */
public class ReservaRepositorioEnMemoria implements ReservaRepositorio {

    private final Map<String, Reserva> reservas = new HashMap<>();

    @Override
    public void guardar(Reserva reserva) {
        reservas.put(reserva.getId(), reserva);
    }

    @Override
    public Optional<Reserva> buscar(String id) {
        return Optional.ofNullable(reservas.get(id));
    }

    @Override
    public Optional<Reserva> buscarPorReferenciaExterna(ReferenciaExterna referencia) {
        return reservas.values().stream()
                .filter(r -> r.getReferenciaExterna().filter(referencia::equals).isPresent())
                .findFirst();
    }

    @Override
    public List<Reserva> activasDe(String codigoApartamento) {
        return reservas.values().stream()
                .filter(Reserva::esActiva)
                .filter(r -> r.getCodigoApartamento().equals(codigoApartamento))
                .toList();
    }

    @Override
    public List<Reserva> activasEnRango(LocalDate desde, LocalDate hasta) {
        return reservas.values().stream()
                .filter(Reserva::esActiva)
                .filter(r -> r.getEstancia().fechaEntrada().isBefore(hasta) && r.getEstancia().fechaSalida().isAfter(desde))
                .toList();
    }

    @Override
    public Pagina<Reserva> listar(FiltroReservas filtro, int pagina) {
        List<Reserva> resultado = reservas.values().stream()
                .filter(cumple(filtro))
                .sorted(Comparator.comparing(Reserva::getCreadaEn).reversed())
                .toList();
        return Pagina.de(resultado, pagina);
    }

    @Override
    public List<Reserva> pendientes() {
        return reservas.values().stream().filter(r -> r.getEstado() == PENDIENTE).toList();
    }

    private static Predicate<Reserva> cumple(FiltroReservas f) {
        return r -> (f.desde() == null || r.getEstancia().fechaSalida().isAfter(f.desde()))
                && (f.hasta() == null || r.getEstancia().fechaEntrada().isBefore(f.hasta()))
                && (f.estado() == null || r.getEstado() == f.estado())
                && (f.codigoApartamento() == null || r.getCodigoApartamento().equals(f.codigoApartamento()))
                && (f.canal() == null || r.getCanal() == f.canal())
                && (f.titular() == null || contiene(r.getTitular().nombre(), f.titular())
                        || contiene(r.getTitular().documento(), f.titular()))
                && (f.idCuenta() == null || r.getTitular().perteneceA(f.idCuenta()));
    }

    private static boolean contiene(String texto, String buscado) {
        return texto != null && texto.toLowerCase().contains(buscado.toLowerCase());
    }
}

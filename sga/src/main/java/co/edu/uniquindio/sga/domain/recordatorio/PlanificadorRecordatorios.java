package co.edu.uniquindio.sga.domain.recordatorio;

import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga.domain.reserva.Reserva;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Decide qué recordatorios corresponden hoy (funcionalidad opcional 6).
 * <ul>
 *     <li><b>Llegada:</b> reservas CONFIRMADA cuya entrada está a {@code diasRecordatorioLlegada} días o
 *     menos (y no es hoy). Si el proceso no corrió algún día, el recordatorio sale al día siguiente.</li>
 *     <li><b>Salida:</b> reservas EN_CURSO cuya salida es mañana.</li>
 * </ul>
 * Solo se recuerda a titulares con correo y una sola vez por reserva y tipo.
 */
public class PlanificadorRecordatorios {

    public record Pendiente(Reserva reserva, TipoRecordatorio tipo, String destinatario, String asunto,
                            String mensaje) {
    }

    /**
     * @param yaEnviado indica si la reserva ya recibió ese tipo de recordatorio
     */
    public List<Pendiente> pendientes(List<Reserva> candidatas, LocalDate hoy, ParametrosAlojamiento parametros,
                                      String nombreAlojamiento, BiPredicate<String, TipoRecordatorio> yaEnviado) {
        List<Pendiente> pendientes = new ArrayList<>();
        for (Reserva reserva : candidatas) {
            String correo = reserva.getTitular().correo();
            if (correo == null || correo.isBlank()) {
                continue;
            }
            tipoQueCorresponde(reserva, hoy, parametros)
                    .filter(tipo -> !yaEnviado.test(reserva.getId(), tipo))
                    .ifPresent(tipo -> pendientes.add(redactar(reserva, tipo, correo, parametros, nombreAlojamiento)));
        }
        return pendientes;
    }

    private static Optional<TipoRecordatorio> tipoQueCorresponde(Reserva reserva, LocalDate hoy,
                                                                 ParametrosAlojamiento parametros) {
        long diasParaEntrada = ChronoUnit.DAYS.between(hoy, reserva.getEstancia().fechaEntrada());
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA && parametros.diasRecordatorioLlegada() > 0
                && diasParaEntrada > 0 && diasParaEntrada <= parametros.diasRecordatorioLlegada()) {
            return Optional.of(TipoRecordatorio.LLEGADA);
        }
        if (reserva.getEstado() == EstadoReserva.EN_CURSO
                && reserva.getEstancia().fechaSalida().equals(hoy.plusDays(1))) {
            return Optional.of(TipoRecordatorio.SALIDA);
        }
        return Optional.empty();
    }

    private static Pendiente redactar(Reserva reserva, TipoRecordatorio tipo, String correo,
                                      ParametrosAlojamiento parametros, String alojamiento) {
        String nombre = reserva.getTitular().nombre();
        if (tipo == TipoRecordatorio.LLEGADA) {
            return new Pendiente(reserva, tipo, correo, "Su llegada a " + alojamiento + " se acerca",
                    "Hola, " + nombre + ". Le esperamos el " + reserva.getEstancia().fechaEntrada()
                            + " en el apartamento " + reserva.getCodigoApartamento() + ". La entrega es desde las "
                            + parametros.horaEntrada() + ". Saldo del folio: " + reserva.getFolio().saldo() + ".");
        }
        return new Pendiente(reserva, tipo, correo, "Mañana termina su estancia en " + alojamiento,
                "Hola, " + nombre + ". Le recordamos que mañana, " + reserva.getEstancia().fechaSalida()
                        + ", la salida es a más tardar a las " + parametros.horaSalida()
                        + ". Saldo del folio: " + reserva.getFolio().saldo() + ".");
    }
}

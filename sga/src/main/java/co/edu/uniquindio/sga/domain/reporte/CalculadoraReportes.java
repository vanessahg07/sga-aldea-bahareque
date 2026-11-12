package co.edu.uniquindio.sga.domain.reporte;

import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;
import co.edu.uniquindio.sga.domain.folio.Pago;
import co.edu.uniquindio.sga.domain.reserva.Canal;
import co.edu.uniquindio.sga.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga.domain.reserva.Reserva;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcula los reportes a partir de las reservas. Una noche cuenta como <b>vendida</b> si
 * pertenece a una reserva CONFIRMADA, EN_CURSO o FINALIZADA: las pendientes aún no están
 * en firme y las canceladas o no-show liberaron sus noches (RN-12).
 */
public class CalculadoraReportes {

    private static final Set<EstadoReserva> VENDIDAS = EnumSet.of(
            EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO, EstadoReserva.FINALIZADA);
    /** Reservas en firme cuya fecha de entrada ya llegó: el grupo llegó o no se presentó. */
    private static final Set<EstadoReserva> CON_LLEGADA_RESUELTA = EnumSet.of(
            EstadoReserva.EN_CURSO, EstadoReserva.FINALIZADA, EstadoReserva.NO_SHOW);
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /**
     * @param apartamentosActivos códigos de los apartamentos a la venta, base de las noches disponibles
     */
    public Reportes.Ocupacion ocupacion(Periodo periodo, List<String> apartamentosActivos, List<Reserva> reservas) {
        RangoFechas noches = periodo.noches();
        Map<String, Long> vendidasPorApartamento = new TreeMap<>();
        apartamentosActivos.forEach(codigo -> vendidasPorApartamento.put(codigo, 0L));
        for (Reserva reserva : vendidas(reservas)) {
            vendidasPorApartamento.merge(reserva.getCodigoApartamento(), nochesEn(reserva, noches), Long::sum);
        }
        long vendidas = vendidasPorApartamento.values().stream().mapToLong(Long::longValue).sum();
        long disponibles = apartamentosActivos.size() * periodo.cantidadNoches();
        List<Reportes.OcupacionApartamento> detalle = vendidasPorApartamento.entrySet().stream()
                .map(e -> new Reportes.OcupacionApartamento(e.getKey(), e.getValue(),
                        porcentaje(e.getValue(), periodo.cantidadNoches())))
                .toList();
        return new Reportes.Ocupacion(periodo, apartamentosActivos.size(), disponibles, vendidas,
                porcentaje(vendidas, disponibles), detalle);
    }

    /** Suma los pagos con fecha dentro del periodo; un reverso resta en el medio del pago original. */
    public Reportes.Ingresos ingresos(Periodo periodo, List<Reserva> reservas) {
        Map<String, List<Pago>> porMedio = reservas.stream()
                .flatMap(r -> r.getFolio().getPagos().stream())
                .filter(p -> periodo.contiene(p.fecha().toLocalDate()))
                .collect(Collectors.groupingBy(p -> p.medio().nombre(), TreeMap::new, Collectors.toList()));
        List<Reportes.IngresoPorMedio> filas = porMedio.entrySet().stream()
                .map(e -> new Reportes.IngresoPorMedio(e.getKey(), sumar(e.getValue()), e.getValue().size()))
                .toList();
        Dinero total = filas.stream().map(Reportes.IngresoPorMedio::total).reduce(Dinero.CERO, Dinero::mas);
        int cantidad = filas.stream().mapToInt(Reportes.IngresoPorMedio::pagos).sum();
        return new Reportes.Ingresos(periodo, filas, total, cantidad);
    }

    /** Reservas creadas en el periodo, por canal y estado actual. */
    public Reportes.ReservasPorCanal porCanal(Periodo periodo, List<Reserva> reservas) {
        List<Reserva> creadas = creadasEn(periodo, reservas);
        List<Reportes.FilaCanal> filas = Arrays.stream(Canal.values()).map(canal -> {
            Map<EstadoReserva, Long> porEstado = new EnumMap<>(EstadoReserva.class);
            Arrays.stream(EstadoReserva.values()).forEach(estado -> porEstado.put(estado, 0L));
            creadas.stream().filter(r -> r.getCanal() == canal)
                    .forEach(r -> porEstado.merge(r.getEstado(), 1L, Long::sum));
            long total = porEstado.values().stream().mapToLong(Long::longValue).sum();
            return new Reportes.FilaCanal(canal, porEstado, total);
        }).toList();
        return new Reportes.ReservasPorCanal(periodo, filas, creadas.size());
    }

    public Reportes.Indicadores indicadores(Periodo periodo, int apartamentosActivos, List<Reserva> reservas) {
        RangoFechas noches = periodo.noches();
        BigDecimal ingreso = BigDecimal.ZERO;
        long vendidas = 0;
        for (Reserva reserva : vendidas(reservas)) {
            long enPeriodo = nochesEn(reserva, noches);
            if (enPeriodo > 0) {
                BigDecimal porNoche = reserva.getCotizacion().valorAlojamiento().valor()
                        .divide(BigDecimal.valueOf(reserva.getEstancia().noches()), 4, RoundingMode.HALF_UP);
                ingreso = ingreso.add(porNoche.multiply(BigDecimal.valueOf(enPeriodo)));
                vendidas += enPeriodo;
            }
        }
        long disponibles = apartamentosActivos * periodo.cantidadNoches();

        List<Reserva> conLlegada = reservas.stream()
                .filter(r -> periodo.contiene(r.getEstancia().fechaEntrada()))
                .filter(r -> CON_LLEGADA_RESUELTA.contains(r.getEstado()))
                .toList();
        long noShow = conLlegada.stream().filter(r -> r.getEstado() == EstadoReserva.NO_SHOW).count();

        List<Reserva> creadas = creadasEn(periodo, reservas);
        long canceladas = creadas.stream().filter(r -> r.getEstado() == EstadoReserva.CANCELADA).count();
        BigDecimal antelacion = creadas.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(creadas.stream()
                        .mapToLong(r -> ChronoUnit.DAYS.between(r.getCreadaEn().toLocalDate(), r.getEstancia().fechaEntrada()))
                        .sum())
                .divide(BigDecimal.valueOf(creadas.size()), 1, RoundingMode.HALF_UP);

        return new Reportes.Indicadores(periodo, Dinero.de(ingreso).redondear(), promedio(ingreso, vendidas),
                promedio(ingreso, disponibles), porcentaje(noShow, conLlegada.size()),
                porcentaje(canceladas, creadas.size()), antelacion);
    }

    // --- Apoyo ---

    private static List<Reserva> vendidas(List<Reserva> reservas) {
        return reservas.stream().filter(r -> VENDIDAS.contains(r.getEstado())).toList();
    }

    private static List<Reserva> creadasEn(Periodo periodo, List<Reserva> reservas) {
        return reservas.stream().filter(r -> periodo.contiene(r.getCreadaEn().toLocalDate())).toList();
    }

    private static long nochesEn(Reserva reserva, RangoFechas periodo) {
        return reserva.getEstancia().rango().listaNoches().stream().filter(periodo::contiene).count();
    }

    private static Dinero sumar(List<Pago> pagos) {
        return pagos.stream().map(Pago::valor).reduce(Dinero.CERO, Dinero::mas);
    }

    private static Dinero promedio(BigDecimal total, long cantidad) {
        return cantidad == 0 ? Dinero.CERO
                : Dinero.de(total.divide(BigDecimal.valueOf(cantidad), 4, RoundingMode.HALF_UP)).redondear();
    }

    static BigDecimal porcentaje(long parte, long total) {
        return total == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(parte).multiply(CIEN).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}

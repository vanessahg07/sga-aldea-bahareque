package co.edu.uniquindio.sga.domain.reporte;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.reserva.Canal;
import co.edu.uniquindio.sga.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga.domain.reserva.GestorReservas;
import co.edu.uniquindio.sga.domain.reserva.Reserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraReportesTest {

    private final CalculadoraReportes calculadora = new CalculadoraReportes();
    private static final Periodo NOVIEMBRE = new Periodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    private static final Periodo OCTUBRE = new Periodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

    @Nested
    @DisplayName("Periodo")
    class Periodos {

        @Test
        @DisplayName("Incluye ambas fechas: del 1 al 30 de noviembre son 30 noches")
        void noches() {
            assertThat(NOVIEMBRE.cantidadNoches()).isEqualTo(30);
            assertThat(new Periodo(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 5)).cantidadNoches()).isEqualTo(1);
        }

        @Test
        @DisplayName("Rechaza un periodo invertido o de más de 366 días")
        void invalido() {
            violaRegla(CodigoError.PERIODO_INVALIDO, () -> new Periodo(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1)));
            violaRegla(CodigoError.PERIODO_INVALIDO, () -> new Periodo(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 2)));
            violaRegla(CodigoError.PERIODO_INVALIDO, () -> new Periodo(null, LocalDate.of(2026, 1, 1)));
        }
    }

    @Nested
    @DisplayName("Ocupación")
    class Ocupacion {

        @Test
        @DisplayName("Noches vendidas / (apartamentos activos × noches del periodo); pendientes y canceladas no cuentan")
        void porcentaje() {
            Reserva confirmada = confirmar(reservaPendiente(apartamento("BH-201"), noviembre(10, 13), adulto("Pedro")));
            Reserva pendiente = reservaPendiente(apartamento("BH-202"), noviembre(10, 20), adulto("Ana"));
            Reserva cancelada = confirmar(reservaPendiente(apartamento("BH-203"), noviembre(1, 30), adulto("Luis")));
            cancelada.cancelar(AHORA);

            Reportes.Ocupacion reporte = calculadora.ocupacion(NOVIEMBRE, List.of("BH-201", "BH-202"),
                    List.of(confirmada, pendiente, cancelada));

            assertThat(reporte.nochesDisponibles()).isEqualTo(60);
            assertThat(reporte.nochesVendidas()).isEqualTo(3);
            assertThat(reporte.porcentaje()).isEqualByComparingTo("5.00");
            assertThat(reporte.porApartamento()).extracting(Reportes.OcupacionApartamento::codigoApartamento)
                    .containsExactly("BH-201", "BH-202");
            assertThat(reporte.porApartamento().getFirst().porcentaje()).isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("Solo cuentan las noches que caen dentro del periodo")
        void nochesQueCruzanElPeriodo() {
            Reserva cruza = confirmar(reservaPendiente(apartamento("BH-201"), estancia(LocalDate.of(2026, 10, 30),
                    LocalDate.of(2026, 11, 3)), adulto("Pedro")));

            assertThat(calculadora.ocupacion(NOVIEMBRE, List.of("BH-201"), List.of(cruza)).nochesVendidas()).isEqualTo(2);
            assertThat(calculadora.ocupacion(OCTUBRE, List.of("BH-201"), List.of(cruza)).nochesVendidas()).isEqualTo(2);
        }

        @Test
        @DisplayName("Sin apartamentos activos la ocupación es cero, no una división por cero")
        void sinInventario() {
            assertThat(calculadora.ocupacion(NOVIEMBRE, List.of(), List.of()).porcentaje()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("Ingresos")
    class Ingresos {

        @Test
        @DisplayName("Suma los pagos del periodo por medio de pago; los cargos no pagados no son ingreso y un reverso resta")
        void porMedio() {
            Reserva reserva = confirmar(reservaPendiente(apartamento("BH-201"), noviembre(10, 13), adulto("Pedro")));
            Dinero anticipo = reserva.getFolio().totalPagos();
            reserva.getFolio().registrarPago(Dinero.de(50_000), EFECTIVO, AHORA.plusDays(1));
            var errado = reserva.getFolio().registrarPago(Dinero.de(20_000), EFECTIVO, AHORA.plusDays(2));
            reserva.getFolio().revertirPago(errado.id(), AHORA.plusDays(2));
            reserva.getFolio().registrarPago(Dinero.de(10_000), EFECTIVO, AHORA.plusMonths(1));

            Reportes.Ingresos reporte = calculadora.ingresos(OCTUBRE, List.of(reserva));

            assertThat(reporte.porMedio()).extracting(Reportes.IngresoPorMedio::medio)
                    .containsExactly("EFECTIVO", "TRANSFERENCIA");
            assertThat(reporte.porMedio().getFirst().total()).isEqualTo(Dinero.de(50_000));
            assertThat(reporte.porMedio().get(1).total()).isEqualTo(anticipo);
            assertThat(reporte.total()).isEqualTo(anticipo.mas(Dinero.de(50_000)));
            assertThat(reporte.total()).isNotEqualTo(reserva.getFolio().totalCargos());
        }
    }

    @Nested
    @DisplayName("Reservas por canal")
    class PorCanal {

        @Test
        @DisplayName("Cuenta las reservas creadas en el periodo por canal, incluidas canceladas y no-show")
        void incluyeCanceladasYNoShow() {
            Reserva directa = confirmar(reservaPendiente(apartamento("BH-201"), noviembre(10, 13), adulto("Pedro")));
            Reserva cancelada = reservaPendiente(apartamento("BH-202"), noviembre(10, 13), adulto("Ana"));
            cancelada.cancelar(AHORA);
            Reserva externa = new GestorReservas().crear(solicitudExterna(noviembre(15, 17), "VE-1"),
                    libre(apartamento("BH-203")), contexto(), AHORA);

            Reportes.ReservasPorCanal reporte = calculadora.porCanal(OCTUBRE, List.of(directa, cancelada, externa));

            assertThat(reporte.total()).isEqualTo(3);
            Reportes.FilaCanal directas = reporte.canales().stream().filter(f -> f.canal() == Canal.DIRECTO).findFirst().orElseThrow();
            assertThat(directas.total()).isEqualTo(2);
            assertThat(directas.porEstado().get(EstadoReserva.CANCELADA)).isEqualTo(1);
            assertThat(reporte.canales()).extracting(Reportes.FilaCanal::canal).containsExactly(Canal.values());
            assertThat(calculadora.porCanal(NOVIEMBRE, List.of(directa)).total()).isZero();
        }
    }

    @Nested
    @DisplayName("Indicadores propios")
    class Indicadores {

        @Test
        @DisplayName("Tarifa promedio por noche, ingreso por noche disponible y tasas de no-show y cancelación")
        void indicadores() {
            Apartamento cafetal = apartamento("BH-201");
            Reserva llego = confirmar(reservaPendiente(cafetal, estancia(HOY, HOY.plusDays(2)), adulto("Pedro")));
            llego.registrarLlegada(HOY, cafetal);
            Reserva noShow = confirmar(reservaPendiente(apartamento("BH-202"), estancia(HOY, HOY.plusDays(2)), adulto("Ana")));
            noShow.declararNoShow(HOY.atTime(22, 30), parametros());
            Reserva cancelada = reservaPendiente(apartamento("BH-203"), estancia(HOY.plusDays(20), HOY.plusDays(22)));
            cancelada.cancelar(AHORA);

            Reportes.Indicadores indicadores = calculadora.indicadores(OCTUBRE, 2, List.of(llego, noShow, cancelada));

            Dinero ingreso = llego.getCotizacion().valorAlojamiento();
            assertThat(indicadores.ingresoAlojamiento()).isEqualTo(ingreso);
            assertThat(indicadores.tarifaPromedioNoche().valor())
                    .isEqualByComparingTo(ingreso.valor().divide(BigDecimal.valueOf(2)));
            assertThat(indicadores.ingresoPorNocheDisponible().valor())
                    .isEqualByComparingTo(ingreso.valor().divide(BigDecimal.valueOf(62), 0, java.math.RoundingMode.HALF_UP));
            assertThat(indicadores.tasaNoShow()).isEqualByComparingTo("50.00");
            assertThat(indicadores.tasaCancelacion()).isEqualByComparingTo("33.33");
            assertThat(indicadores.antelacionPromedioDias()).isEqualByComparingTo("6.7");
        }
    }
}

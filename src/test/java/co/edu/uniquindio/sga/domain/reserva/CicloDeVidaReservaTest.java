package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.folio.AutorizacionCierre;
import co.edu.uniquindio.sga.domain.folio.TipoCargo;
import co.edu.uniquindio.sga.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.politica.TramoCancelacion;
import co.edu.uniquindio.sga.domain.tarifa.Tarifa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static co.edu.uniquindio.sga.domain.reserva.EstadoReserva.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Reserva base: BH-201, 10 al 12 de noviembre, 2 adultos → 340.000; anticipo 30 % → 102.000. */
class CicloDeVidaReservaTest {

    private static final LocalDate ENTRADA = LocalDate.of(2026, 11, 10);

    private final GestorReservas gestor = new GestorReservas();
    private final Apartamento apto = apartamento("BH-201");

    private Reserva pendiente() {
        return reservaPendiente(apto, noviembre(10, 12), adulto("Pedro"));
    }

    private Reserva confirmada() {
        return confirmar(pendiente());
    }

    private Reserva enCurso() {
        Reserva reserva = confirmada();
        reserva.registrarLlegada(ENTRADA, apto);
        return reserva;
    }

    /** Reserva PENDIENTE creada sin hora estimada de llegada y con el anticipo ya pagado. */
    private Reserva pendienteSinHoraConAnticipo() {
        SolicitudReserva sinHora = new SolicitudReserva(titular(), List.of(), noviembre(10, 12), null,
                Canal.PORTAL, null, 0);
        Reserva reserva = gestor.crear(sinHora, libre(apto), contexto(), AHORA);
        reserva.getFolio().registrarPago(Dinero.de(51_000), TRANSFERENCIA, AHORA);
        return reserva;
    }

    private static PoliticaCancelacion politicaV2(LocalDateTime vigenteDesde) {
        return politicaV1().nuevaVersion(vigenteDesde, List.of(
                new TramoCancelacion(30, BigDecimal.ZERO),
                new TramoCancelacion(0, BigDecimal.valueOf(100))), BigDecimal.valueOf(100));
    }

    @Nested
    @DisplayName("Confirmación")
    class Confirmacion {

        @Test
        @DisplayName("RN-09 violada: sin hora estimada de llegada no se confirma")
        void sinHoraEstimada() {
            // Arrange
            Reserva reserva = pendienteSinHoraConAnticipo();

            // Act
            ExcepcionNegocio error = capturar(() -> reserva.confirmar(parametros()));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.HORA_LLEGADA_REQUERIDA);
            assertThat(reserva.getEstado()).isEqualTo(PENDIENTE);
        }

        @Test
        @DisplayName("RN-09 cumplida: al registrar la hora estimada ya se puede confirmar")
        void conHoraEstimada() {
            // Arrange
            Reserva reserva = pendienteSinHoraConAnticipo();
            reserva.registrarHoraEstimadaLlegada(LocalTime.of(17, 30));

            // Act
            reserva.confirmar(parametros());

            // Assert
            assertThat(reserva.getEstado()).isEqualTo(CONFIRMADA);
        }

        @Test
        @DisplayName("Sin el anticipo del 30 % no se confirma")
        void anticipoInsuficiente() {
            // Arrange
            Reserva reserva = pendiente();
            reserva.getFolio().registrarPago(Dinero.de(50_000), EFECTIVO, AHORA);

            // Act
            ExcepcionNegocio error = capturar(() -> reserva.confirmar(parametros()));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.ANTICIPO_INSUFICIENTE);
        }

        @Test
        @DisplayName("El anticipo exigido es el 30 % del valor de la estancia")
        void anticipoRequerido() {
            // Arrange
            Reserva reserva = pendiente();

            // Act
            Dinero anticipo = reserva.anticipoRequerido(parametros());

            // Assert
            assertThat(anticipo).isEqualTo(Dinero.de(102_000));
        }

        @Test
        @DisplayName("Con el anticipo pagado se confirma")
        void anticipoPagado() {
            // Arrange
            Reserva reserva = pendiente();
            reserva.getFolio().registrarPago(Dinero.de(102_000), TRANSFERENCIA, AHORA);

            // Act
            reserva.confirmar(parametros());

            // Assert
            assertThat(reserva.getEstado()).isEqualTo(CONFIRMADA);
        }
    }

    @Nested
    @DisplayName("Registro de llegada")
    class Registro {

        @Test
        @DisplayName("RN-10 violada: antes de la fecha de entrada")
        void antesDeLaEntrada() {
            // Arrange
            Reserva reserva = confirmada();

            // Act
            ExcepcionNegocio error = capturar(() -> reserva.registrarLlegada(ENTRADA.minusDays(1), apto));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.REGISTRO_ANTES_DE_ENTRADA);
        }

        @Test
        @DisplayName("RN-10 violada: reserva que no está CONFIRMADA")
        void reservaNoConfirmada() {
            // Arrange
            Reserva reserva = pendiente();

            // Act
            ExcepcionNegocio error = capturar(() -> reserva.registrarLlegada(ENTRADA, apto));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.TRANSICION_INVALIDA);
        }

        @Test
        @DisplayName("RN-11 violada: apartamento que no está PREPARADO")
        void apartamentoNoPreparado() {
            // Arrange
            Reserva reserva = confirmada();
            apto.declararFueraDeServicio();

            // Act
            ExcepcionNegocio error = capturar(() -> reserva.registrarLlegada(ENTRADA, apto));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.APARTAMENTO_NO_PREPARADO);
            assertThat(reserva.getEstado()).isEqualTo(CONFIRMADA);
        }

        @Test
        @DisplayName("RN-10 y RN-11 cumplidas: la reserva pasa a EN_CURSO y el apartamento a OCUPADO")
        void registroExitoso() {
            // Arrange
            Reserva reserva = confirmada();

            // Act
            reserva.registrarLlegada(ENTRADA, apto);

            // Assert
            assertThat(reserva.getEstado()).isEqualTo(EN_CURSO);
            assertThat(apto.getEstadoOperativo()).isEqualTo(EstadoOperativo.OCUPADO);
        }
    }

    @Nested
    @DisplayName("Salida")
    class Salida {

        @Test
        @DisplayName("RN-17: con saldo pendiente y sin autorización no se completa la salida")
        void saldoPendienteSinAutorizacion() {
            // Arrange
            Reserva reserva = enCurso();

            // Act
            ExcepcionNegocio error = capturar(() -> reserva.registrarSalida(apto, null));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.FOLIO_CON_SALDO);
            assertThat(reserva.getEstado()).isEqualTo(EN_CURSO);
        }

        @Test
        @DisplayName("Con autorización del administrador se completa y queda registrada")
        void saldoPendienteConAutorizacion() {
            // Arrange
            Reserva reserva = enCurso();
            AutorizacionCierre autorizacion = new AutorizacionCierre("admin@aldea.co", "Pago acordado a 30 días",
                    ENTRADA.plusDays(2).atTime(11, 0));

            // Act
            reserva.registrarSalida(apto, autorizacion);

            // Assert
            assertThat(reserva.getEstado()).isEqualTo(FINALIZADA);
            assertThat(reserva.getFolio().isCerrado()).isTrue();
            assertThat(reserva.getFolio().getAutorizacionCierre()).contains(autorizacion);
            assertThat(apto.getEstadoOperativo()).isEqualTo(EstadoOperativo.PENDIENTE_PREPARACION);
        }

        @Test
        @DisplayName("Con el folio a paz y salvo se completa sin autorización")
        void folioPagado() {
            // Arrange
            Reserva reserva = enCurso();
            reserva.getFolio().registrarPago(reserva.getFolio().saldo(), EFECTIVO, ENTRADA.plusDays(2).atTime(10, 0));

            // Act
            reserva.registrarSalida(apto, null);

            // Assert
            assertThat(reserva.getEstado()).isEqualTo(FINALIZADA);
            assertThat(reserva.getFolio().getAutorizacionCierre()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Cancelación")
    class Cancelacion {

        @Test
        @DisplayName("Con 15 días o más no hay retención: lo pagado queda como saldo a favor")
        void sinRetencion() {
            // Arrange: se cancela el 1 de octubre, 40 días antes de la entrada
            Reserva reserva = confirmada();

            // Act
            Dinero retencion = reserva.cancelar(AHORA);

            // Assert
            assertThat(retencion).isEqualTo(Dinero.CERO);
            assertThat(reserva.getEstado()).isEqualTo(CANCELADA);
            assertThat(reserva.getFolio().saldo()).isEqualTo(Dinero.de(-102_000));
        }

        @Test
        @DisplayName("Entre 7 y 14 días se retiene el 30 %")
        void retencionTreintaPorCiento() {
            // Arrange
            Reserva reserva = confirmada();
            LocalDateTime nueveDiasAntes = LocalDateTime.of(2026, 11, 1, 9, 0);

            // Act
            Dinero retencion = reserva.cancelar(nueveDiasAntes);

            // Assert
            assertThat(retencion).isEqualTo(Dinero.de(102_000));
            assertThat(reserva.getFolio().saldo()).isEqualTo(Dinero.CERO);
            assertThat(reserva.getFolio().getCargos()).extracting(c -> c.tipo()).contains(TipoCargo.PENALIDAD);
        }

        @Test
        @DisplayName("Con menos de 7 días se retiene el 60 %")
        void retencionSesentaPorCiento() {
            // Arrange
            Reserva reserva = confirmada();
            LocalDateTime cincoDiasAntes = LocalDateTime.of(2026, 11, 5, 9, 0);

            // Act
            Dinero retencion = reserva.cancelar(cincoDiasAntes);

            // Assert
            assertThat(retencion).isEqualTo(Dinero.de(204_000));
            assertThat(reserva.getFolio().saldo()).isEqualTo(Dinero.de(102_000));
        }

        @Test
        @DisplayName("La salida anticipada no es una cancelación: EN_CURSO no puede cancelarse")
        void enCursoNoSeCancela() {
            // Arrange
            Reserva reserva = enCurso();

            // Act
            ExcepcionNegocio error = capturar(() -> reserva.cancelar(ENTRADA.atTime(18, 0)));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.TRANSICION_INVALIDA);
        }

        @Test
        @DisplayName("RN-13: la retención usa la política congelada, no la vigente")
        void politicaCongelada() {
            // Arrange: después de crear la reserva se publica una v2 que retendría el 100 %
            Reserva reserva = confirmada();
            PoliticaCancelacion v2 = politicaV2(LocalDateTime.of(2026, 10, 15, 0, 0));
            Dinero retencionSegunV2 = v2.retencionPorCancelacion(reserva.getCotizacion().valorTotal(),
                    LocalDate.of(2026, 11, 5), ENTRADA);

            // Act
            Dinero retencion = reserva.cancelar(LocalDateTime.of(2026, 11, 5, 9, 0));

            // Assert
            assertThat(retencionSegunV2).isEqualTo(Dinero.de(340_000));
            assertThat(retencion).isEqualTo(Dinero.de(204_000));
            assertThat(reserva.getPolitica().version()).isEqualTo(1);
        }

        @Test
        @DisplayName("RN-13: una reserva creada después del cambio queda con la nueva versión")
        void reservaNuevaTomaLaPoliticaVigente() {
            // Arrange
            ContextoReserva conV2 = new ContextoReserva(parametros(), calendario(), tarifas(),
                    politicaV2(LocalDateTime.of(2026, 10, 1, 0, 0)));

            // Act
            Reserva reserva = gestor.crear(solicitud(noviembre(10, 12)), libre(apto), conV2, AHORA);

            // Assert
            assertThat(reserva.getPolitica().version()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("No-show")
    class NoShow {

        @Test
        @DisplayName("No se declara antes de la hora límite (22:00) del día de entrada")
        void antesDeLaHoraLimite() {
            // Arrange
            Reserva reserva = confirmada();

            // Act
            ExcepcionNegocio error = capturar(() -> reserva.declararNoShow(ENTRADA.atTime(21, 59), parametros()));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.NO_SHOW_ANTICIPADO);
        }

        @Test
        @DisplayName("Desde la hora límite se declara y retiene el 30 % de la política congelada")
        void desdeLaHoraLimite() {
            // Arrange
            Reserva reserva = confirmada();

            // Act
            Dinero retencion = reserva.declararNoShow(ENTRADA.atTime(22, 0), parametros());

            // Assert
            assertThat(reserva.getEstado()).isEqualTo(NO_SHOW);
            assertThat(retencion).isEqualTo(Dinero.de(102_000));
            assertThat(reserva.getFolio().saldo()).isEqualTo(Dinero.CERO);
        }

        @Test
        @DisplayName("Una reserva PENDIENTE no puede pasar a NO_SHOW")
        void pendienteNoPasaANoShow() {
            // Arrange
            Reserva reserva = pendiente();

            // Act
            ExcepcionNegocio error = capturar(() -> reserva.declararNoShow(ENTRADA.atTime(23, 0), parametros()));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.TRANSICION_INVALIDA);
        }
    }

    @Nested
    @DisplayName("RN-12: las reservas que dejan de estar activas liberan sus noches")
    class LiberacionDeNoches {

        private Reserva venderDeNuevo(Reserva terminada) {
            return gestor.crear(solicitud(noviembre(10, 12)), ocupacion(apto, List.of(terminada)), contexto(), AHORA);
        }

        @Test
        @DisplayName("Tras cancelación")
        void trasCancelacion() {
            // Arrange
            Reserva cancelada = confirmada();
            cancelada.cancelar(AHORA);

            // Act
            Reserva nueva = venderDeNuevo(cancelada);

            // Assert
            assertThat(cancelada.esActiva()).isFalse();
            assertThat(nueva.esActiva()).isTrue();
        }

        @Test
        @DisplayName("Tras no-show")
        void trasNoShow() {
            // Arrange
            Reserva noShow = confirmada();
            noShow.declararNoShow(ENTRADA.atTime(22, 0), parametros());

            // Act
            Reserva nueva = venderDeNuevo(noShow);

            // Assert
            assertThat(noShow.esActiva()).isFalse();
            assertThat(nueva.esActiva()).isTrue();
        }

        @Test
        @DisplayName("Tras vencimiento")
        void trasVencimiento() {
            // Arrange
            Reserva vencida = pendiente();
            vencida.vencerSiCorresponde(AHORA.plusHours(24), parametros());

            // Act
            Reserva nueva = venderDeNuevo(vencida);

            // Assert
            assertThat(vencida.esActiva()).isFalse();
            assertThat(nueva.esActiva()).isTrue();
        }
    }

    @Nested
    @DisplayName("RN-21: vencimiento de reservas pendientes")
    class Vencimiento {

        @Test
        @DisplayName("Superado el plazo de 24 h pasa a CANCELADA sin penalidad")
        void vencida() {
            // Arrange
            Reserva reserva = pendiente();

            // Act
            boolean vencio = reserva.vencerSiCorresponde(AHORA.plusHours(25), parametros());

            // Assert
            assertThat(vencio).isTrue();
            assertThat(reserva.getEstado()).isEqualTo(CANCELADA);
            assertThat(reserva.getFolio().saldo()).isEqualTo(Dinero.CERO);
            assertThat(reserva.getFolio().getCargos()).extracting(c -> c.tipo()).doesNotContain(TipoCargo.PENALIDAD);
        }

        @Test
        @DisplayName("Dentro del plazo sigue PENDIENTE")
        void dentroDelPlazo() {
            // Arrange
            Reserva reserva = pendiente();

            // Act
            boolean vencio = reserva.vencerSiCorresponde(AHORA.plusHours(23), parametros());

            // Assert
            assertThat(vencio).isFalse();
            assertThat(reserva.getEstado()).isEqualTo(PENDIENTE);
        }

        @Test
        @DisplayName("Una reserva confirmada no vence")
        void confirmadaNoVence() {
            // Arrange
            Reserva reserva = confirmada();

            // Act
            boolean vencio = reserva.vencerSiCorresponde(AHORA.plusDays(3), parametros());

            // Assert
            assertThat(vencio).isFalse();
            assertThat(reserva.getEstado()).isEqualTo(CONFIRMADA);
        }
    }

    @Test
    @DisplayName("RN-22: un cambio posterior de tarifas no altera el valor de la reserva")
    void valorCongelado() {
        // Arrange
        ContextoReserva contexto = contexto();
        Reserva reserva = gestor.crear(solicitud(noviembre(10, 12), adulto("Pedro")), libre(apto), contexto, AHORA);

        // Act: el administrador sube la tarifa BASE después de creada la reserva
        ContextoReserva conTarifaNueva = new ContextoReserva(contexto.parametros(), contexto.calendario(),
                contexto.tarifas().conTarifa(new Tarifa("BH-201", BASE, Dinero.de(200_000), AHORA.plusHours(1))),
                contexto.politicaVigente());

        // Assert
        assertThat(conTarifaNueva.tarifas().valorPara("BH-201", BASE)).isEqualTo(Dinero.de(200_000));
        assertThat(reserva.getCotizacion().valorTotal()).isEqualTo(Dinero.de(340_000));
        assertThat(reserva.getFolio().saldo()).isEqualTo(Dinero.de(340_000));
    }
}

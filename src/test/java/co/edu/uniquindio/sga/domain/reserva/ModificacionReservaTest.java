package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.folio.Cargo;
import co.edu.uniquindio.sga.domain.folio.TipoCargo;
import co.edu.uniquindio.sga.domain.tarifa.Tarifa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/** RN-14 y RN-22. Reserva base: BH-201, 10 al 12 de noviembre, 2 adultos → 340.000. */
class ModificacionReservaTest {

    private final GestorReservas gestor = new GestorReservas();
    private final Apartamento apto = apartamento("BH-201");
    private final Ocupante pedro = adulto("Pedro");

    private Reserva reserva() {
        return reservaPendiente(apto, noviembre(10, 12), pedro);
    }

    private Cargo ultimoCargo(Reserva reserva) {
        return reserva.getFolio().getCargos().getLast();
    }

    @Test
    @DisplayName("Alargar la estancia registra un ajuste positivo")
    void ajustePositivo() {
        // Arrange
        Reserva reserva = reserva();

        // Act
        gestor.modificar(reserva, noviembre(10, 13), List.of(pedro), 0, ocupacion(apto, List.of(reserva)),
                contexto(), AHORA);

        // Assert
        assertThat(reserva.getCotizacion().valorTotal()).isEqualTo(Dinero.de(510_000));
        assertThat(ultimoCargo(reserva).tipo()).isEqualTo(TipoCargo.AJUSTE);
        assertThat(ultimoCargo(reserva).valor()).isEqualTo(Dinero.de(170_000));
        assertThat(reserva.getFolio().saldo()).isEqualTo(Dinero.de(510_000));
    }

    @Test
    @DisplayName("Reducir el grupo registra un ajuste negativo")
    void ajusteNegativo() {
        // Arrange
        Reserva reserva = reserva();

        // Act
        gestor.modificar(reserva, noviembre(10, 12), List.of(), 0, ocupacion(apto, List.of(reserva)),
                contexto(), AHORA);

        // Assert
        assertThat(ultimoCargo(reserva).valor()).isEqualTo(Dinero.de(-170_000));
        assertThat(reserva.getFolio().saldo()).isEqualTo(Dinero.de(170_000));
    }

    @Test
    @DisplayName("Mover la estancia sobre sus propias noches no choca consigo misma")
    void noChocaConsigoMisma() {
        // Arrange
        Reserva reserva = reserva();

        // Act
        gestor.modificar(reserva, noviembre(11, 13), List.of(pedro), 0, ocupacion(apto, List.of(reserva)),
                contexto(), AHORA);

        // Assert
        assertThat(reserva.getEstancia()).isEqualTo(noviembre(11, 13));
    }

    @Test
    @DisplayName("Revalida el solapamiento: no puede moverse sobre otra reserva activa")
    void revalidaSolapamiento() {
        // Arrange
        Reserva reserva = reserva();
        Reserva otra = reservaPendiente(apto, noviembre(13, 15));

        // Act
        ExcepcionNegocio error = capturar(() -> gestor.modificar(reserva, noviembre(10, 14), List.of(pedro), 0,
                ocupacion(apto, List.of(reserva, otra)), contexto(), AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.NOCHES_NO_DISPONIBLES);
        assertThat(reserva.getEstancia()).isEqualTo(noviembre(10, 12));
        assertThat(reserva.getFolio().getCargos()).hasSize(1);
    }

    @Test
    @DisplayName("Revalida la capacidad")
    void revalidaCapacidad() {
        // Arrange
        Reserva reserva = reserva();
        List<Ocupante> grupoGrande = List.of(pedro, adulto("A"), adulto("B"), adulto("C"));

        // Act
        ExcepcionNegocio error = capturar(() -> gestor.modificar(reserva, noviembre(10, 12), grupoGrande, 0,
                ocupacion(apto, List.of(reserva)), contexto(), AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.CAPACIDAD_EXCEDIDA);
    }

    @Test
    @DisplayName("Revalida la fecha de entrada")
    void revalidaFechaEntrada() {
        // Arrange
        Reserva reserva = reserva();
        Estancia desdeAyer = estancia(HOY.minusDays(1), HOY.plusDays(1));

        // Act
        ExcepcionNegocio error = capturar(() -> gestor.modificar(reserva, desdeAyer, List.of(pedro), 0,
                ocupacion(apto, List.of(reserva)), contexto(), AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.FECHA_ENTRADA_PASADA);
    }

    @Test
    @DisplayName("Cambiar de apartamento recalcula con la tarifa del nuevo")
    void cambioDeApartamento() {
        // Arrange
        Reserva reserva = reserva();
        Apartamento guadua = apartamento("BH-101");

        // Act
        gestor.modificar(reserva, noviembre(10, 12), List.of(pedro), 0, libre(guadua), contexto(), AHORA);

        // Assert
        assertThat(reserva.getCodigoApartamento()).isEqualTo("BH-101");
        assertThat(ultimoCargo(reserva).valor()).isEqualTo(Dinero.de(40_000));
    }

    @Test
    @DisplayName("Recalcula con las tarifas vigentes al momento de la modificación, sin cambiar la política")
    void usaTarifasVigentes() {
        // Arrange
        ContextoReserva contexto = contexto();
        Reserva reserva = gestor.crear(solicitud(noviembre(10, 12), pedro), libre(apto), contexto, AHORA);
        ContextoReserva conTarifaNueva = new ContextoReserva(contexto.parametros(), contexto.calendario(),
                contexto.tarifas().conTarifa(
                        new Tarifa("BH-201", BASE, Dinero.de(100_000), LocalDateTime.of(2026, 10, 1, 11, 0))),
                contexto.politicaVigente());

        // Act
        gestor.modificar(reserva, noviembre(10, 12), List.of(pedro), 0, ocupacion(apto, List.of(reserva)),
                conTarifaNueva, AHORA.plusHours(2));

        // Assert
        assertThat(ultimoCargo(reserva).valor()).isEqualTo(Dinero.de(60_000));
        assertThat(reserva.getPolitica().version()).isEqualTo(1);
    }

    @Test
    @DisplayName("Una reserva EN_CURSO no se modifica")
    void enCursoNoSeModifica() {
        // Arrange
        Reserva reserva = confirmar(reserva());
        reserva.registrarLlegada(noviembre(10, 12).fechaEntrada(), apto);

        // Act
        ExcepcionNegocio error = capturar(() -> gestor.modificar(reserva, noviembre(10, 13), List.of(pedro), 0,
                ocupacion(apto, List.of(reserva)), contexto(), AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.RESERVA_NO_MODIFICABLE);
    }

    @Test
    @DisplayName("Una reserva cancelada no se modifica")
    void canceladaNoSeModifica() {
        // Arrange
        Reserva reserva = reserva();
        reserva.cancelar(AHORA);

        // Act
        ExcepcionNegocio error = capturar(() -> gestor.modificar(reserva, noviembre(10, 13), List.of(pedro), 0,
                ocupacion(apto, List.of(reserva)), contexto(), AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.RESERVA_NO_MODIFICABLE);
    }
}

package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static co.edu.uniquindio.sga.domain.Escenario.capturar;
import static co.edu.uniquindio.sga.domain.reserva.EstadoReserva.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("RN-08: la reserva solo transita entre los estados permitidos")
class EstadoReservaTest {

    private static final Map<EstadoReserva, Set<EstadoReserva>> PERMITIDAS = Map.of(
            PENDIENTE, Set.of(CONFIRMADA, CANCELADA),
            CONFIRMADA, Set.of(EN_CURSO, CANCELADA, NO_SHOW),
            EN_CURSO, Set.of(FINALIZADA),
            FINALIZADA, Set.of(),
            CANCELADA, Set.of(),
            NO_SHOW, Set.of());

    static Stream<Arguments> transicionesPermitidas() {
        return combinaciones().filter(a -> PERMITIDAS.get((EstadoReserva) a.get()[0]).contains(a.get()[1]));
    }

    static Stream<Arguments> transicionesNoPermitidas() {
        return combinaciones().filter(a -> !PERMITIDAS.get((EstadoReserva) a.get()[0]).contains(a.get()[1]));
    }

    private static Stream<Arguments> combinaciones() {
        return Arrays.stream(values()).flatMap(desde -> Arrays.stream(values()).map(hacia -> Arguments.of(desde, hacia)));
    }

    @ParameterizedTest(name = "{0} → {1} es válida")
    @MethodSource("transicionesPermitidas")
    void transicionPermitida(EstadoReserva desde, EstadoReserva hacia) {
        // Arrange: el par (desde, hacia) llega de la tabla de transiciones permitidas

        // Act
        Throwable error = catchThrowable(() -> desde.validarTransicionA(hacia));

        // Assert
        assertThat(error).isNull();
    }

    @ParameterizedTest(name = "{0} → {1} se rechaza")
    @MethodSource("transicionesNoPermitidas")
    void transicionNoPermitida(EstadoReserva desde, EstadoReserva hacia) {
        // Arrange: el par (desde, hacia) no está en la tabla de transiciones permitidas

        // Act
        ExcepcionNegocio error = capturar(() -> desde.validarTransicionA(hacia));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.TRANSICION_INVALIDA);
    }

    @Test
    @DisplayName("Activas: PENDIENTE, CONFIRMADA y EN_CURSO; las demás son terminales")
    void estadosActivosYTerminales() {
        // Arrange
        List<EstadoReserva> todos = List.of(values());

        // Act
        List<EstadoReserva> activos = todos.stream().filter(EstadoReserva::esActiva).toList();
        List<EstadoReserva> terminales = todos.stream().filter(EstadoReserva::esTerminal).toList();

        // Assert
        assertThat(activos).containsExactlyInAnyOrder(PENDIENTE, CONFIRMADA, EN_CURSO);
        assertThat(terminales).containsExactlyInAnyOrder(FINALIZADA, CANCELADA, NO_SHOW);
    }
}

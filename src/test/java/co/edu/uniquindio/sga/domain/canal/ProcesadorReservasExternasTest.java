package co.edu.uniquindio.sga.domain.canal;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.reserva.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

class ProcesadorReservasExternasTest {

    private final ProcesadorReservasExternas procesador = new ProcesadorReservasExternas(new GestorReservas());
    private final Apartamento apto = apartamento("BH-201");

    @Test
    @DisplayName("Reserva externa sin conflicto se acepta con canal EXTERNO y su referencia")
    void aceptada() {
        // Arrange
        SolicitudReserva mensaje = solicitudExterna(noviembre(10, 12), "VE-100");

        // Act
        ResultadoReservaExterna resultado = procesador.procesar(mensaje, Optional.empty(), libre(apto), contexto(),
                AHORA);

        // Assert
        assertThat(resultado).isInstanceOf(ResultadoReservaExterna.Aceptada.class);
        Reserva reserva = ((ResultadoReservaExterna.Aceptada) resultado).reserva();
        assertThat(reserva.getCanal()).isEqualTo(Canal.EXTERNO);
        assertThat(reserva.getReferenciaExterna()).contains(new ReferenciaExterna("ViajaEje", "VE-100"));
    }

    @Test
    @DisplayName("RN-18: una reserva externa en conflicto se rechaza, se registra y no toca la existente")
    void conflictoRechazado() {
        // Arrange
        Reserva existente = confirmar(reservaPendiente(apto, noviembre(10, 12), adulto("Pedro")));
        SolicitudReserva mensaje = solicitudExterna(noviembre(11, 13), "VE-200");

        // Act
        ResultadoReservaExterna resultado = procesador.procesar(mensaje, Optional.empty(),
                ocupacion(apto, List.of(existente)), contexto(), AHORA);

        // Assert
        assertThat(resultado).isInstanceOf(ResultadoReservaExterna.RechazadaPorConflicto.class);
        ConflictoCanal conflicto = ((ResultadoReservaExterna.RechazadaPorConflicto) resultado).conflicto();
        assertThat(conflicto.motivo()).isEqualTo(CodigoError.NOCHES_NO_DISPONIBLES);
        assertThat(conflicto.referencia().idExterno()).isEqualTo("VE-200");
        assertThat(conflicto.codigoApartamento()).isEqualTo("BH-201");
        assertThat(existente.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(existente.getEstancia()).isEqualTo(noviembre(10, 12));
        assertThat(existente.getFolio().getCargos()).hasSize(1);
    }

    @Test
    @DisplayName("RN-19: el mismo mensaje recibido dos veces no crea dos reservas")
    void mensajeRepetido() {
        // Arrange
        SolicitudReserva mensaje = solicitudExterna(noviembre(10, 12), "VE-300");
        Reserva primera = ((ResultadoReservaExterna.Aceptada) procesador.procesar(mensaje, Optional.empty(),
                libre(apto), contexto(), AHORA)).reserva();

        // Act
        ResultadoReservaExterna repetido = procesador.procesar(mensaje, Optional.of(primera),
                ocupacion(apto, List.of(primera)), contexto(), AHORA.plusMinutes(1));

        // Assert
        assertThat(repetido).isInstanceOf(ResultadoReservaExterna.Duplicada.class);
        assertThat(((ResultadoReservaExterna.Duplicada) repetido).reservaExistente()).isSameAs(primera);
    }

    @Test
    @DisplayName("Un error que no es de disponibilidad no se registra como conflicto")
    void otrosErroresSePropagan() {
        // Arrange
        SolicitudReserva excedeCapacidad = solicitudExterna(noviembre(10, 12), "VE-400",
                adulto("A"), adulto("B"), adulto("C"), adulto("D"));

        // Act
        ExcepcionNegocio error = capturar(() -> procesador.procesar(excedeCapacidad, Optional.empty(),
                libre(apto), contexto(), AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.CAPACIDAD_EXCEDIDA);
    }

    @Test
    @DisplayName("Solo acepta solicitudes del canal EXTERNO con referencia")
    void soloCanalExterno() {
        // Arrange
        SolicitudReserva directa = solicitud(noviembre(10, 12));

        // Act
        ExcepcionNegocio error = capturar(() -> procesador.procesar(directa, Optional.empty(), libre(apto),
                contexto(), AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.RESERVA_EXTERNA_INVALIDA);
    }
}

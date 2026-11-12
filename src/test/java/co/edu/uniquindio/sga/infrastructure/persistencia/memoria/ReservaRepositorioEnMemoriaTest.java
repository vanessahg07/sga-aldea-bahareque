package co.edu.uniquindio.sga.infrastructure.persistencia.memoria;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.comun.Pagina;
import co.edu.uniquindio.sga.domain.reserva.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

class ReservaRepositorioEnMemoriaTest {

    private final ReservaRepositorioEnMemoria repositorio = new ReservaRepositorioEnMemoria();
    private final GestorReservas gestor = new GestorReservas();
    private final Apartamento cafetal = apartamento("BH-201");

    @Test
    @DisplayName("Una reserva guardada se recupera por su identificador")
    void guardarYBuscar() {
        // Arrange
        Reserva reserva = reservaPendiente(cafetal, noviembre(10, 12));
        repositorio.guardar(reserva);

        // Act
        Optional<Reserva> encontrada = repositorio.buscar(reserva.getId());

        // Assert
        assertThat(encontrada).contains(reserva);
    }

    @Test
    @DisplayName("RN-19: una reserva externa se encuentra por canal e identificador externo")
    void buscarPorReferenciaExterna() {
        // Arrange
        Reserva externa = gestor.crear(solicitudExterna(noviembre(10, 12), "VE-500"), libre(cafetal), contexto(), AHORA);
        repositorio.guardar(externa);

        // Act
        Optional<Reserva> encontrada = repositorio.buscarPorReferenciaExterna(new ReferenciaExterna("ViajaEje", "VE-500"));

        // Assert
        assertThat(encontrada).contains(externa);
    }

    @Test
    @DisplayName("RN-12: las reservas activas del apartamento excluyen las canceladas")
    void activasExcluyeCanceladas() {
        // Arrange
        Reserva activa = reservaPendiente(cafetal, noviembre(10, 12));
        Reserva cancelada = reservaPendiente(cafetal, noviembre(20, 22));
        cancelada.cancelar(AHORA);
        repositorio.guardar(activa);
        repositorio.guardar(cancelada);

        // Act
        List<Reserva> activas = repositorio.activasDe("BH-201");

        // Assert
        assertThat(activas).containsExactly(activa);
    }

    @Test
    @DisplayName("El listado filtra por estado y ordena de la más reciente a la más antigua")
    void listarFiltradoYOrdenado() {
        // Arrange
        Reserva antigua = gestor.crear(solicitud(noviembre(10, 12)), libre(cafetal), contexto(), AHORA);
        Reserva reciente = gestor.crear(solicitud(noviembre(20, 22)), libre(cafetal), contexto(), AHORA.plusHours(1));
        Reserva cancelada = gestor.crear(solicitud(noviembre(24, 26)), libre(cafetal), contexto(), AHORA.plusHours(2));
        cancelada.cancelar(AHORA.plusHours(3));
        List.of(antigua, reciente, cancelada).forEach(repositorio::guardar);
        FiltroReservas soloPendientes = new FiltroReservas(null, null, EstadoReserva.PENDIENTE, null, null, null, null);

        // Act
        Pagina<Reserva> pagina = repositorio.listar(soloPendientes, 0);

        // Assert
        assertThat(pagina.contenido()).containsExactly(reciente, antigua);
        assertThat(pagina.totalElementos()).isEqualTo(2);
    }

    @Test
    @DisplayName("RN-21: las pendientes son solo las reservas en estado PENDIENTE")
    void pendientes() {
        // Arrange
        Reserva pendiente = reservaPendiente(cafetal, noviembre(10, 12));
        Reserva confirmada = confirmar(reservaPendiente(cafetal, noviembre(20, 22)));
        repositorio.guardar(pendiente);
        repositorio.guardar(confirmada);

        // Act
        List<Reserva> pendientes = repositorio.pendientes();

        // Assert
        assertThat(pendientes).containsExactly(pendiente);
    }
}

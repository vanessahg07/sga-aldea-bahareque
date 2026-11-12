package co.edu.uniquindio.sga.domain.recordatorio;

import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.reserva.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlanificadorRecordatoriosTest {

    private final PlanificadorRecordatorios planificador = new PlanificadorRecordatorios();

    private List<PlanificadorRecordatorios.Pendiente> pendientes(List<Reserva> reservas, ParametrosAlojamiento p,
                                                                 Set<String> yaEnviados) {
        return planificador.pendientes(reservas, HOY, p, "Aldea Bahareque",
                (id, tipo) -> yaEnviados.contains(id + tipo));
    }

    @Test
    @DisplayName("Recuerda la llegada de una reserva confirmada que entra dentro de los días configurados")
    void recordatorioDeLlegada() {
        Reserva enDosDias = confirmar(reservaPendiente(apartamento("BH-201"), estancia(HOY.plusDays(2), HOY.plusDays(4)),
                adulto("Pedro")));
        Reserva enTresDias = confirmar(reservaPendiente(apartamento("BH-202"), estancia(HOY.plusDays(3), HOY.plusDays(5)),
                adulto("Ana")));
        Reserva hoy = confirmar(reservaPendiente(apartamento("BH-203"), estancia(HOY, HOY.plusDays(2)), adulto("Luis")));
        Reserva pendiente = reservaPendiente(apartamento("BH-301"), estancia(HOY.plusDays(1), HOY.plusDays(3)));

        var resultado = pendientes(List.of(enDosDias, enTresDias, hoy, pendiente), parametros(), Set.of());

        assertThat(resultado).singleElement().satisfies(p -> {
            assertThat(p.reserva()).isSameAs(enDosDias);
            assertThat(p.tipo()).isEqualTo(TipoRecordatorio.LLEGADA);
            assertThat(p.destinatario()).isEqualTo("laura@correo.com");
            assertThat(p.mensaje()).contains(enDosDias.getEstancia().fechaEntrada().toString()).contains("15:00");
        });
    }

    @Test
    @DisplayName("Recuerda la salida el día anterior a una reserva en curso")
    void recordatorioDeSalida() {
        Apartamento cafetal = apartamento("BH-201");
        Reserva saleManana = confirmar(reservaPendiente(cafetal, estancia(HOY, HOY.plusDays(1)), adulto("Pedro")));
        saleManana.registrarLlegada(HOY, cafetal);

        var resultado = pendientes(List.of(saleManana), parametros(), Set.of());

        assertThat(resultado).singleElement().satisfies(p -> {
            assertThat(p.tipo()).isEqualTo(TipoRecordatorio.SALIDA);
            assertThat(p.mensaje()).contains("11:00");
        });
    }

    @Test
    @DisplayName("No repite un recordatorio ya enviado ni lo envía a titulares sin correo")
    void sinDuplicadosNiSinCorreo() {
        Reserva enviada = confirmar(reservaPendiente(apartamento("BH-201"), estancia(HOY.plusDays(1), HOY.plusDays(3)),
                adulto("Pedro")));
        Titular sinCorreo = new Titular("Julián Castaño", "10222333", null, "3161239876", LocalDate.of(1995, 10, 15));
        Reserva otra = confirmar(new GestorReservas().crear(new SolicitudReserva(sinCorreo, List.of(),
                estancia(HOY.plusDays(1), HOY.plusDays(3)), LocalTime.of(16, 0), Canal.DIRECTO, null, 0),
                libre(apartamento("BH-202")), contexto(), AHORA));

        var resultado = pendientes(List.of(enviada, otra), parametros(), Set.of(enviada.getId() + TipoRecordatorio.LLEGADA));

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Con cero días configurados no se envían recordatorios de llegada")
    void desactivado() {
        ParametrosAlojamiento sinRecordatorio = new ParametrosAlojamiento(5, LocalTime.of(15, 0), LocalTime.of(11, 0), 3,
                24, LocalTime.of(22, 0), BigDecimal.valueOf(30), 1, Dinero.de(40_000), 7, BigDecimal.TEN, 0);
        Reserva manana = confirmar(reservaPendiente(apartamento("BH-201"), estancia(HOY.plusDays(1), HOY.plusDays(3)),
                adulto("Pedro")));

        assertThat(pendientes(List.of(manana), sinRecordatorio, Set.of())).isEmpty();
    }
}

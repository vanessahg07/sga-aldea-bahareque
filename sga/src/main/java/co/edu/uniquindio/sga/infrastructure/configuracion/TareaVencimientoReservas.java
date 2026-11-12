package co.edu.uniquindio.sga.infrastructure.configuracion;

import co.edu.uniquindio.sga.application.reserva.ReservaServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** RN-21: cancela sin intervención manual las reservas pendientes que superan el plazo de confirmación. */
@Component
public class TareaVencimientoReservas {

    private static final Logger LOG = LoggerFactory.getLogger(TareaVencimientoReservas.class);

    private final ReservaServicio reservas;

    public TareaVencimientoReservas(ReservaServicio reservas) {
        this.reservas = reservas;
    }

    @Scheduled(fixedDelayString = "${sga.reservas.intervalo-vencimiento-ms:60000}",
            initialDelayString = "${sga.reservas.intervalo-vencimiento-ms:60000}")
    public void vencerPendientes() {
        try {
            int vencidas = reservas.vencerPendientes();
            if (vencidas > 0) {
                LOG.info("Reservas pendientes vencidas y canceladas: {}", vencidas);
            }
        } catch (RuntimeException e) {
            LOG.error("Falló la revisión de reservas pendientes vencidas", e);
        }
    }
}

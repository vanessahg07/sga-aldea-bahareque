package co.edu.uniquindio.sga.application.comun;

import co.edu.uniquindio.sga.domain.comun.Notificador;
import co.edu.uniquindio.sga.domain.reserva.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envía los avisos al titular de una reserva. Una falla al notificar se registra y nunca
 * detiene la operación de negocio que la origina (I-05).
 */
@Component
public class AvisosReserva {

    private static final Logger LOG = LoggerFactory.getLogger(AvisosReserva.class);

    private final Notificador notificador;

    public AvisosReserva(Notificador notificador) {
        this.notificador = notificador;
    }

    /** @return {@code true} si el aviso se entregó al notificador; {@code false} si no hay correo o falló */
    public boolean enviar(Reserva reserva, String asunto, String mensaje) {
        return enviar(reserva.getTitular().correo(), asunto, mensaje);
    }

    public boolean enviar(String correo, String asunto, String mensaje) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        try {
            notificador.notificar(new Notificador.Notificacion(correo, asunto, mensaje));
            return true;
        } catch (RuntimeException e) {
            LOG.warn("No se pudo notificar a {} ({}): {}", correo, asunto, e.getMessage());
            return false;
        }
    }
}

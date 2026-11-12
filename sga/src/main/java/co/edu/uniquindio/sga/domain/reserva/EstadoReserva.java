package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.util.Set;

/** Ciclo de vida de la reserva (sección 8 del enunciado). */
public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    EN_CURSO,
    FINALIZADA,
    CANCELADA,
    NO_SHOW;

    /** Las reservas activas retienen disponibilidad. */
    public boolean esActiva() {
        return this == PENDIENTE || this == CONFIRMADA || this == EN_CURSO;
    }

    public boolean esTerminal() {
        return !esActiva();
    }

    public boolean puedeTransitarA(EstadoReserva destino) {
        return destinosPermitidos().contains(destino);
    }

    /** RN-08: toda transición no listada se rechaza. */
    public void validarTransicionA(EstadoReserva destino) {
        ExcepcionNegocio.exigir(puedeTransitarA(destino), CodigoError.TRANSICION_INVALIDA, this + " → " + destino);
    }

    private Set<EstadoReserva> destinosPermitidos() {
        return switch (this) {
            case PENDIENTE -> Set.of(CONFIRMADA, CANCELADA);
            case CONFIRMADA -> Set.of(EN_CURSO, CANCELADA, NO_SHOW);
            case EN_CURSO -> Set.of(FINALIZADA);
            case FINALIZADA, CANCELADA, NO_SHOW -> Set.of();
        };
    }
}

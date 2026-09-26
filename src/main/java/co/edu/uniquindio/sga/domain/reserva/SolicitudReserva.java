package co.edu.uniquindio.sga.domain.reserva;

import java.time.LocalTime;
import java.util.List;

/**
 * Datos con los que se pide crear una reserva.
 *
 * @param acompanantes          ocupantes distintos del titular (el titular siempre es ocupante)
 * @param horaEstimadaLlegada   puede faltar al crear, pero es obligatoria para confirmar (RN-09)
 * @param referenciaExterna     solo para el canal EXTERNO
 */
public record SolicitudReserva(Titular titular, List<Ocupante> acompanantes, Estancia estancia,
                               LocalTime horaEstimadaLlegada, Canal canal, ReferenciaExterna referenciaExterna,
                               int mascotas) {

    public SolicitudReserva {
        acompanantes = acompanantes == null ? List.of() : List.copyOf(acompanantes);
    }
}

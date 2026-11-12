package co.edu.uniquindio.sga.domain.canal;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.reserva.Canal;
import co.edu.uniquindio.sga.domain.reserva.ContextoReserva;
import co.edu.uniquindio.sga.domain.reserva.GestorReservas;
import co.edu.uniquindio.sga.domain.reserva.OcupacionApartamento;
import co.edu.uniquindio.sga.domain.reserva.Reserva;
import co.edu.uniquindio.sga.domain.reserva.SolicitudReserva;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Recibe reservas de canales externos garantizando idempotencia (RN-19) y que un
 * conflicto nunca sobrescriba una reserva vigente (RN-18).
 */
public class ProcesadorReservasExternas {

    /** Motivos que constituyen un conflicto de canal (colisión con el inventario ya vendido o bloqueado). */
    private static final Set<CodigoError> MOTIVOS_CONFLICTO = Set.of(
            CodigoError.NOCHES_NO_DISPONIBLES,
            CodigoError.APARTAMENTO_BLOQUEADO,
            CodigoError.TIEMPO_PREPARACION_INSUFICIENTE);

    private final GestorReservas gestor;

    public ProcesadorReservasExternas(GestorReservas gestor) {
        this.gestor = gestor;
    }

    /**
     * @param yaRecibida reserva existente con la misma referencia externa, si la hay
     */
    public ResultadoReservaExterna procesar(SolicitudReserva solicitud, Optional<Reserva> yaRecibida,
                                            OcupacionApartamento ocupacion, ContextoReserva contexto,
                                            LocalDateTime ahora) {
        ExcepcionNegocio.exigir(solicitud.canal() == Canal.EXTERNO && solicitud.referenciaExterna() != null,
                CodigoError.RESERVA_EXTERNA_INVALIDA);

        if (yaRecibida.isPresent()) {
            return new ResultadoReservaExterna.Duplicada(yaRecibida.get());
        }
        try {
            return new ResultadoReservaExterna.Aceptada(gestor.crear(solicitud, ocupacion, contexto, ahora));
        } catch (ExcepcionNegocio e) {
            if (!MOTIVOS_CONFLICTO.contains(e.getCodigo())) {
                throw e;
            }
            ConflictoCanal conflicto = new ConflictoCanal(UUID.randomUUID().toString(), solicitud.referenciaExterna(),
                    ocupacion.apartamento().getCodigo(), solicitud.estancia(), solicitud.titular().nombre(),
                    e.getCodigo(), ahora, null);
            return new ResultadoReservaExterna.RechazadaPorConflicto(conflicto);
        }
    }
}

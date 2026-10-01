package co.edu.uniquindio.sga.domain.canal;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.reserva.Estancia;
import co.edu.uniquindio.sga.domain.reserva.ReferenciaExterna;

import java.time.LocalDateTime;

/**
 * Reserva externa rechazada por colisionar con una reserva vigente, pendiente de revisión del administrador.
 *
 * @param titular  nombre del titular que envió el canal, para que el administrador pueda contactarlo
 * @param revision revisión del administrador; {@code null} mientras el conflicto está pendiente
 */
public record ConflictoCanal(String id, ReferenciaExterna referencia, String codigoApartamento, Estancia estancia,
                             String titular, CodigoError motivo, LocalDateTime registradoEn, Revision revision) {

    /** Constancia de que el administrador revisó el conflicto y qué decidió. */
    public record Revision(String autor, String nota, LocalDateTime fecha) {

        public Revision {
            ExcepcionNegocio.exigir(autor != null && !autor.isBlank() && nota != null && !nota.isBlank()
                    && fecha != null, CodigoError.AUTORIZACION_INVALIDA, "la revisión debe indicar autor y nota");
        }
    }

    public boolean estaPendiente() {
        return revision == null;
    }

    /** Registra la revisión del administrador. Un conflicto se revisa una sola vez. */
    public ConflictoCanal revisar(String autor, String nota, LocalDateTime fecha) {
        ExcepcionNegocio.exigir(estaPendiente(), CodigoError.CONFLICTO_YA_REVISADO);
        return new ConflictoCanal(id, referencia, codigoApartamento, estancia, titular, motivo, registradoEn,
                new Revision(autor, nota, fecha));
    }
}

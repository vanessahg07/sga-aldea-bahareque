package co.edu.uniquindio.sga.domain.recordatorio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Constancia de un recordatorio enviado. Se guarda para que cada reserva reciba cada tipo
 * de recordatorio una sola vez, aunque el proceso se ejecute varias veces el mismo día.
 */
public record Recordatorio(String id, String idReserva, TipoRecordatorio tipo, String destinatario, String asunto,
                           LocalDateTime enviadoEn) {

    public static Recordatorio de(String idReserva, TipoRecordatorio tipo, String destinatario, String asunto,
                                  LocalDateTime enviadoEn) {
        return new Recordatorio(UUID.randomUUID().toString(), idReserva, tipo, destinatario, asunto, enviadoEn);
    }
}

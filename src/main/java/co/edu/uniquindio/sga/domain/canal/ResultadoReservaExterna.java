package co.edu.uniquindio.sga.domain.canal;

import co.edu.uniquindio.sga.domain.reserva.Reserva;

/** Resultado de procesar una reserva enviada por un canal externo. */
public sealed interface ResultadoReservaExterna {

    /** La reserva se creó. */
    record Aceptada(Reserva reserva) implements ResultadoReservaExterna { }

    /** El mensaje ya se había recibido: se devuelve la reserva existente sin crear otra (RN-19). */
    record Duplicada(Reserva reservaExistente) implements ResultadoReservaExterna { }

    /** Colisiona con una reserva vigente: se rechaza y se registra el conflicto (RN-18). */
    record RechazadaPorConflicto(ConflictoCanal conflicto) implements ResultadoReservaExterna { }
}

package co.edu.uniquindio.sga.domain.canal;

/** Tipos de evento que registra la bitácora de canales. */
public enum TipoEventoCanal {
    CONSULTA_DISPONIBILIDAD,
    RESERVA_ACEPTADA,
    /** El mismo (canal, idExterno) llegó otra vez: no se creó una segunda reserva (RN-19). */
    RESERVA_DUPLICADA,
    /** Colisión con el inventario vendido o bloqueado: rechazada y registrada como conflicto (RN-18). */
    RESERVA_EN_CONFLICTO,
    /** Rechazada por una regla de negocio distinta de la disponibilidad (capacidad, fechas, etc.). */
    RESERVA_RECHAZADA,
    CANCELACION_ACEPTADA,
    /** La cancelación ya se había aplicado: se responde con el mismo resultado sin repetirla. */
    CANCELACION_REPETIDA,
    CANCELACION_RECHAZADA,
    CREDENCIAL_INVALIDA
}

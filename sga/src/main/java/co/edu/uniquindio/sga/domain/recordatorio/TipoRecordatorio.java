package co.edu.uniquindio.sga.domain.recordatorio;

/** Momento del ciclo de la reserva que se recuerda al titular. */
public enum TipoRecordatorio {
    /** Días antes de la entrada, según el parámetro del alojamiento. */
    LLEGADA,
    /** El día anterior a la salida. */
    SALIDA
}

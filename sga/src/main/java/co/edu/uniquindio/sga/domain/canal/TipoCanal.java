package co.edu.uniquindio.sga.domain.canal;

/** Tipo de canal de venta externo que se conecta al SGA. */
public enum TipoCanal {
    /** Agencia de viajes en línea que publica el inventario y envía reservas. */
    AGENCIA_EN_LINEA,
    /** Agencia de viajes tradicional o mayorista que reserva por integración. */
    AGENCIA_DE_VIAJES,
    /** Metabuscador que redirige al huésped y confirma la reserva por la API. */
    METABUSCADOR
}

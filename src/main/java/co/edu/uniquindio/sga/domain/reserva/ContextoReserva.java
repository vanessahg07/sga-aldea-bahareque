package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga.domain.tarifa.TablaTarifas;

/** Configuración vigente del alojamiento que interviene al crear o modificar una reserva. */
public record ContextoReserva(ParametrosAlojamiento parametros, CalendarioTemporadas calendario,
                              TablaTarifas tarifas, PoliticaCancelacion politicaVigente) {
}

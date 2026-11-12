package co.edu.uniquindio.sga.application.comun;

import co.edu.uniquindio.sga.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga.domain.alojamiento.AlojamientoRepositorio;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.politica.PoliticaRepositorio;
import co.edu.uniquindio.sga.domain.reserva.ContextoReserva;
import co.edu.uniquindio.sga.domain.tarifa.TarifaRepositorio;
import co.edu.uniquindio.sga.domain.tarifa.TemporadaRepositorio;
import org.springframework.stereotype.Component;

/** Reúne la configuración vigente del alojamiento que necesitan los casos de uso de reservas. */
@Component
public class ContextoVigente {

    private final AlojamientoRepositorio alojamientos;
    private final TemporadaRepositorio temporadas;
    private final TarifaRepositorio tarifas;
    private final PoliticaRepositorio politicas;

    public ContextoVigente(AlojamientoRepositorio alojamientos, TemporadaRepositorio temporadas,
                           TarifaRepositorio tarifas, PoliticaRepositorio politicas) {
        this.alojamientos = alojamientos;
        this.temporadas = temporadas;
        this.tarifas = tarifas;
        this.politicas = politicas;
    }

    public Alojamiento alojamiento() {
        return alojamientos.obtener().orElseThrow(() -> new ExcepcionNegocio(CodigoError.ALOJAMIENTO_NO_CONFIGURADO));
    }

    public ContextoReserva contextoReserva() {
        return new ContextoReserva(alojamiento().getParametros(), temporadas.calendario(), tarifas.tabla(),
                politicas.vigente().orElseThrow(() -> new ExcepcionNegocio(CodigoError.POLITICA_NO_CONFIGURADA)));
    }
}

package co.edu.uniquindio.sga.domain.apartamento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;

import java.util.List;
import java.util.UUID;

/**
 * Decisión administrativa que impide vender un apartamento en un rango de noches.
 * No cambia por sí sola el estado operativo.
 */
public record Bloqueo(String id, String codigoApartamento, RangoFechas rango, String motivo) {

    public Bloqueo {
        ExcepcionNegocio.exigir(id != null && codigoApartamento != null && rango != null,
                CodigoError.BLOQUEO_INVALIDO);
        ExcepcionNegocio.exigir(motivo != null && !motivo.isBlank(), CodigoError.BLOQUEO_INVALIDO, "motivo");
    }

    /**
     * Registra un bloqueo nuevo. No puede cubrir noches que ya tengan reservas activas.
     *
     * @param estanciasActivas estancias de las reservas activas del apartamento
     */
    public static Bloqueo registrar(String codigoApartamento, RangoFechas rango, String motivo,
                                    List<RangoFechas> estanciasActivas) {
        ExcepcionNegocio.exigir(estanciasActivas.stream().noneMatch(rango::seSolapaCon),
                CodigoError.BLOQUEO_SOBRE_RESERVAS);
        return new Bloqueo(UUID.randomUUID().toString(), codigoApartamento, rango, motivo);
    }

    public boolean afecta(RangoFechas estancia) {
        return rango.seSolapaCon(estancia);
    }
}
